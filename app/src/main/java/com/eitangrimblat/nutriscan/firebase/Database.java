package com.eitangrimblat.nutriscan.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Utility class for interacting with Firebase Firestore and Firebase Storage.
 */
public class Database {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageRef = storage.getReference();

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_RECIPES = "recipes";
    public static final String COLLECTION_STATS = "stats";

    public static final String FIELD_ALLERGIES = "allergies";

    public static final String PRODUCT_FIELD_NAME = "name";
    public static final String PRODUCT_FIELD_BRANDS = "brands";
    public static final String PRODUCT_FIELD_QUANTITY = "quantity";
    public static final String PRODUCT_FIELD_ENERGY = "energy";
    public static final String PRODUCT_FIELD_FAT = "fat";
    public static final String PRODUCT_FIELD_SALT = "salt";
    public static final String PRODUCT_FIELD_SUGARS = "sugars";

    public static final String RECIPE_FIELD_NAME = "name";
    public static final String RECIPE_FIELD_DESC = "description";
    public static final String RECIPE_FIELD_IMAGE = "image";
    public static final String RECIPE_FIELD_INSTRUCTIONS = "instructions";
    public static final String RECIPE_FIELD_PRODUCTIDS = "product_ids";

    public static final String ALLERGY_KEY_DAIRY = "dairy";
    public static final String ALLERGY_KEY_EGGS = "eggs";
    public static final String ALLERGY_KEY_FISH = "fish";
    public static final String ALLERGY_KEY_GLUTEN = "gluten";
    public static final String ALLERGY_KEY_NUTS = "nuts";
    public static final String ALLERGY_KEY_SESAME = "sesame";

    public static final String DOCUMENT_CACHE  = "cache";
    public static final String DOCUMENT_CACHE_PRODUCT_IDS     = "product_ids";
    public static final String DOCUMENT_CACHE_OVERALL_SCORE   = "overall_score";
    public static final String DOCUMENT_CACHE_NUTRIENT1       = "nutrient1";
    public static final String DOCUMENT_CACHE_NUTRIENT2       = "nutrient2";
    public static final String DOCUMENT_CACHE_NUTRIENT3       = "nutrient3";
    public static final String DOCUMENT_CACHE_INSIGHT1_TITLE  = "insight1_title";
    public static final String DOCUMENT_CACHE_INSIGHT1_CONTENT= "insight1_content";
    public static final String DOCUMENT_CACHE_INSIGHT2_TITLE  = "insight2_title";
    public static final String DOCUMENT_CACHE_INSIGHT2_CONTENT= "insight2_content";
    public static final String DOCUMENT_CACHE_INSIGHT3_TITLE  = "insight3_title";
    public static final String DOCUMENT_CACHE_INSIGHT3_CONTENT= "insight3_content";
    public static final String DOCUMENT_CACHE_SPECIAL_SCORE   = "special_score";
    public static final String DOCUMENT_CACHE_SPECIAL_TITLE   = "special_score_title";

    /**
     * Saves or merges data into a specific Firestore document.
     *
     * @param collection         The collection containing the document.
     * @param document           The document ID.
     * @param data               The data to save.
     * @param onCompleteRunnable Callback for success or failure.
     */
    public static void saveData(CollectionReference collection, String document, Map<String, Object> data, OnCompleteRunnable<Void> onCompleteRunnable) {
        try {
            collection.document(document)
            .set(data, SetOptions.merge())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Eitan Debug Database", "Database saved data: " + collection.getPath() + "/" + document + ": " + data);
                    onCompleteRunnable.onComplete(null, true, null);
                } else {
                    onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                }
            });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Retrieves data from a specific Firestore document.
     *
     * @param collection         The collection containing the document.
     * @param document           The document ID.
     * @param onCompleteRunnable Callback providing the DocumentSnapshot.
     */
    public static void loadData(CollectionReference collection, String document, OnCompleteRunnable<DocumentSnapshot> onCompleteRunnable) {
        try {
            collection.document(document)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Eitan Debug Database", "Database loaded data: " + collection.getPath() + "/" + document);
                    onCompleteRunnable.onComplete(task.getResult(), true, null);
                } else {
                    onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                }
            });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Loads all documents within a specified Firestore collection.
     *
     * @param collection         The collection to load.
     * @param onCompleteRunnable Callback providing the QuerySnapshot.
     */
    public static void loadCollection(CollectionReference collection, OnCompleteRunnable<QuerySnapshot> onCompleteRunnable) {
        try {
            collection.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("Eitan Debug Database", "Database loaded collection: " + collection.getPath());
                            onCompleteRunnable.onComplete(task.getResult(), true, null);
                        } else {
                            onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                        }
                    });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Deletes all documents in a collection using a batch operation.
     *
     * @param collection         The collection to clear.
     * @param onCompleteRunnable Callback for completion status.
     */
    public static void deleteCollection(CollectionReference collection, OnCompleteRunnable<Void> onCompleteRunnable) {
        try {
            collection.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnCompleteListener(batchTask -> {
                        if (batchTask.isSuccessful()) {
                            Log.d("Eitan Debug Database", "Database collection cleared: " + collection.getPath());
                            onCompleteRunnable.onComplete(null, true, null);
                        } else {
                            onCompleteRunnable.onComplete(null, false, batchTask.getException().getMessage());
                        }
                    });
                } else {
                    onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                }
            });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Adds a new document with an auto-generated ID to a collection.
     *
     * @param collection         The target collection.
     * @param data               The data for the new document.
     * @param onCompleteRunnable Callback providing the new document ID.
     */
    public static void addDocument(CollectionReference collection, Map<String, Object> data, OnCompleteRunnable<String> onCompleteRunnable) {
        try {
            collection.add(data)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Eitan Debug Database", "Database added document: " + collection.getPath());
                    onCompleteRunnable.onComplete(task.getResult().getId(), true, null);
                } else {
                    onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                }
            });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Deletes a specific document from a collection.
     *
     * @param collection         The collection containing the document.
     * @param documentId         The ID of the document to delete.
     * @param onCompleteRunnable Callback for completion status.
     */
    public static void deleteDocument(CollectionReference collection, String documentId, OnCompleteRunnable<Void> onCompleteRunnable) {
        try {
            collection.document(documentId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Eitan Debug Database", "Database deleted document: " + collection.getPath() + "/" + documentId);
                        onCompleteRunnable.onComplete(null, true, null);
                    } else {
                        onCompleteRunnable.onComplete(null, false, task.getException().getMessage());
                    }
                });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onCompleteRunnable.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Gets a reference to a top-level Firestore collection.
     *
     * @param collection The collection name.
     * @return A CollectionReference for the specified name.
     */
    public static CollectionReference getCollection(String collection) {
        return db.collection(collection);
    }

    /**
     * Uploads a file (byte array) to Firebase Storage under the current user's path.
     *
     * @param file       The file content as bytes.
     * @param fileName   The name of the file in storage.
     * @param onComplete Callback providing the download URI.
     */
    public static void storeFile(byte[] file, String fileName, OnCompleteRunnable<Uri> onComplete) {
        String uid = Auth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child("users/" + uid + "/" + fileName);

        fileRef.putBytes(file)
        .continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return fileRef.getDownloadUrl();
        })
        .addOnSuccessListener(  uri -> {
            Log.d("Eitan Debug Database", "Storage file uploaded: " + fileName);
            onComplete.onComplete(uri, true, null);
        })
        .addOnFailureListener(e -> {
            Log.d("Eitan Debug Database", e.toString());
            onComplete.onComplete(null, false, e.getMessage());
        });
    }

    /**
     * Downloads a file from Firebase Storage as a byte array.
     *
     * @param fileName   The name of the file to load.
     * @param onComplete Callback providing the byte array.
     */
    public static void loadFile(String fileName, OnCompleteRunnable<byte[]> onComplete) {
        String uid = Auth.getCurrentUser().getUid();

        storageRef.child("users/" + uid + "/" + fileName).getBytes(5 * 1024 * 1024)
        .addOnSuccessListener(bytes -> {
            Log.d("Eitan Debug Database", "Storage file loaded bytes: " + fileName);
            onComplete.onComplete(bytes, true, null);
        })
        .addOnFailureListener(e -> {
            Log.d("Eitan Debug Database", e.toString());
            onComplete.onComplete(null, false, e.getMessage());
        });
    }

    /**
     * Compresses and uploads a Bitmap image to Firebase Storage.
     *
     * @param image      The Bitmap to store.
     * @param imageName  The name for the image file.
     * @param quality    Compression quality (0-100).
     * @param onComplete Callback providing the download URI.
     */
    public static void storeImage(Bitmap image, String imageName, int quality, OnCompleteRunnable<Uri> onComplete) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        storeFile(baos.toByteArray(), imageName, onComplete);
    }

    /**
     * Loads an image from Firebase Storage and decodes it into a Bitmap.
     *
     * @param imageName  The name of the image to load.
     * @param onComplete Callback providing the decoded Bitmap.
     */
    public static void loadImage(String imageName, OnCompleteRunnable<Bitmap> onComplete) {
        loadFile(imageName, (bytes, success, exception) -> {
            if (!success) {
                Log.d("Eitan Debug Database", "Failed to load image: null image");
                onComplete.onComplete(null, false, "null image");
                return;
            }

            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (image != null) {
                onComplete.onComplete(image, true, null);
            } else {
                onComplete.onComplete(null, false, "Failed to decode bitmap");
            }
        });
    }

    /**
     * Deletes a file from Firebase Storage by name.
     *
     * @param fileName   The name of the file to delete.
     * @param onComplete Callback for completion status.
     */
    public static void deleteFile(String fileName, OnCompleteRunnable<Void> onComplete) {
        try {
            String uid = Auth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child("users/" + uid + "/" + fileName);

            fileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Eitan Debug Database", "Storage file deleted: " + fileName);
                    onComplete.onComplete(null, true, null);
                })
                .addOnFailureListener(e -> {
                    Log.d("Eitan Debug Database", e.toString());
                    onComplete.onComplete(null, false, e.getMessage());
                });
        } catch (NullPointerException e) {
            Log.d("Eitan Debug Database", e.toString());
            onComplete.onComplete(null, false, e.getMessage());
        }
    }

    /**
     * Deletes a file from Firebase Storage using its download URI.
     *
     * @param uri        The URI of the file to delete.
     * @param onComplete Callback for completion status.
     */
    public static void deleteFile(Uri uri, OnCompleteRunnable<Void> onComplete) {
        try {
            deleteFile(storage.getReferenceFromUrl(uri.toString()).getName(), onComplete);
        } catch (IllegalArgumentException e) {
            onComplete.onComplete(null, false, e.getMessage());
        }
    }
}
