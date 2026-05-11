package com.example.foottest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe pour uploader des images sur ImgBB (alternative gratuite à Firebase Storage)
 * 100% gratuit, sans carte bancaire, stockage illimité !
 */
public class ImgBBUploader {
    private static final String TAG = "ImgBBUploader";

    // ✅ Clé API gratuite (tu peux utiliser celle-ci ou créer la tienne sur imgbb.com)
    private static final String API_KEY = "7d1f83171e573bb8a0ed3beae6b25635"; // Exemple - crée la tienne !
    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";

    private final ExecutorService executorService;
    private final Context context;

    public ImgBBUploader(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(3); // 3 uploads simultanés max
    }

    /**
     * Upload plusieurs images et retourne leurs URLs
     */
    public void uploadImages(List<Uri> imageUris, OnUploadListener listener) {
        if (imageUris == null || imageUris.isEmpty()) {
            listener.onFailure("Aucune image à uploader");
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        AtomicInteger uploadCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        int totalImages = imageUris.size();

        Log.d(TAG, "🔄 Début upload de " + totalImages + " images sur ImgBB");

        for (int i = 0; i < imageUris.size(); i++) {
            final Uri imageUri = imageUris.get(i);
            final int index = i + 1;

            executorService.execute(() -> {
                try {
                    Log.d(TAG, "📤 Upload image " + index + "/" + totalImages);
                    Log.d(TAG, "   URI: " + imageUri.toString());

                    // Convertir l'image en Base64
                    String base64Image = uriToBase64(imageUri);

                    if (base64Image == null) {
                        Log.e(TAG, "   ❌ Impossible de lire l'image");
                        checkIfFinished(uploadCount.incrementAndGet(), successCount.get(),
                                totalImages, uploadedUrls, listener);
                        return;
                    }

                    Log.d(TAG, "   ✅ Image convertie en Base64");

                    // Uploader sur ImgBB
                    String imageUrl = uploadToImgBB(base64Image);

                    if (imageUrl != null) {
                        synchronized (uploadedUrls) {
                            uploadedUrls.add(imageUrl);
                        }
                        successCount.incrementAndGet();
                        Log.d(TAG, "   ✅ Upload réussi!");
                        Log.d(TAG, "   URL: " + imageUrl);
                    } else {
                        Log.e(TAG, "   ❌ Échec upload");
                    }

                    checkIfFinished(uploadCount.incrementAndGet(), successCount.get(),
                            totalImages, uploadedUrls, listener);

                } catch (Exception e) {
                    Log.e(TAG, "   ❌ Exception: " + e.getMessage());
                    e.printStackTrace();
                    checkIfFinished(uploadCount.incrementAndGet(), successCount.get(),
                            totalImages, uploadedUrls, listener);
                }
            });
        }
    }

    /**
     * Convertit une URI en Base64
     */
    private String uriToBase64(Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            // Charger l'image
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) return null;

            // Redimensionner si trop grande (max 1024px)
            bitmap = resizeBitmap(bitmap, 1024);

            // Convertir en Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Erreur conversion Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Redimensionne une image si elle est trop grande
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Upload l'image sur ImgBB via leur API
     */
    private String uploadToImgBB(String base64Image) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(UPLOAD_URL + "?key=" + API_KEY);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Préparer les données
            String postData = "image=" + URLEncoder.encode(base64Image, "UTF-8");

            // Envoyer
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(postData.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            // Lire la réponse
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                StringBuilder response = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    response.append(new String(buffer, 0, bytesRead, "UTF-8"));
                }
                inputStream.close();

                // Parser la réponse JSON
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getBoolean("success")) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    return data.getString("url"); // URL de l'image uploadée
                }
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur upload ImgBB: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * Vérifie si tous les uploads sont terminés
     */
    private void checkIfFinished(int uploadCount, int successCount, int totalImages,
                                 List<String> uploadedUrls, OnUploadListener listener) {
        if (uploadCount == totalImages) {
            Log.d(TAG, "🏁 Terminé: " + successCount + "/" + totalImages + " réussies");

            if (uploadedUrls.isEmpty()) {
                listener.onFailure("Aucune image n'a pu être uploadée");
            } else {
                listener.onSuccess(new ArrayList<>(uploadedUrls));
            }
        }
    }

    /**
     * Nettoyer les ressources
     */
    public void shutdown() {
        executorService.shutdown();
    }

    // Interface pour les callbacks
    public interface OnUploadListener {
        void onSuccess(List<String> imageUrls);
        void onFailure(String errorMessage);
    }
}