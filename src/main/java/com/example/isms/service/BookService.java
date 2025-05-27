package com.example.isms.service;

import com.example.isms.model.book;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BookService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    @Autowired
    StudentSearchService studentSearchService;


    public void addNewBook(book newBook) {
        try {
            DocumentReference docRef = firestore.collection("books").document(newBook.getId());
            DocumentSnapshot docSnapshot = docRef.get().get();

            Map<String, Object> bookData = new HashMap<>();
            bookData.put("id", newBook.getId());
            bookData.put("name", newBook.getName());

            firestore.collection("books").document(newBook.getId()).set(bookData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add or update book: " + e.getMessage(), e);
        }
    }

    public void addBooksInBulk(List<book> books) {
        for (book b : books) {
            try {
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("id", b.getId());
                bookData.put("name", b.getName());

                firestore.collection("books").document(b.getId()).set(bookData);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add or update book: " + e.getMessage(), e);
            }
        }
    }

    public book getBookById(String id) {
        try {
            DocumentReference docRef = firestore.collection("books").document(id);
            return docRef.get().get().toObject(book.class);
        } catch (Exception e) {
            throw new RuntimeException("Book not found with ID: " + id, e);
        }
    }

    public Map<String, Object> getBookByName(String title) {
        try {
            String lowerTitle = title.toLowerCase();
            ApiFuture<QuerySnapshot> future = firestore.collection("books").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<QueryDocumentSnapshot> matchedBooks = new ArrayList<>();
            String actualBookName = null;

            for (QueryDocumentSnapshot doc : documents) {
                String name = doc.getString("name");
                if (name != null && name.toLowerCase().equals(lowerTitle)) {
                    matchedBooks.add(doc);
                    actualBookName = name;
                }
            }

            int availableCopies = matchedBooks.size();

            if (availableCopies > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("bookName", actualBookName);
                result.put("availableCopies", availableCopies);
                return result;
            } else {
                throw new RuntimeException("No book found with title: " + title);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving book by name: " + title, e);
        }
    }

    public void issueBook(String studentId, String bookId) {
        try {
            book bookToIssue = getBookById(bookId);

            Map<String, Object> issueDetails = new HashMap<>();
            issueDetails.put("bookId", bookId);
            issueDetails.put("bookName", bookToIssue.getName());
            issueDetails.put("issueDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

            firestore.collection("studentbook")
                    .document(studentId)
                    .collection("issuedbook")
                    .document(bookId)
                    .set(issueDetails);

            firestore.collection("books").document(bookId).delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue book: " + e.getMessage(), e);
        }
    }

    public void returnBook(String studentId, String bookId) {
        try {
            DocumentSnapshot issuedBookDoc = firestore.collection("studentbook")
                    .document(studentId)
                    .collection("issuedbook")
                    .document(bookId)
                    .get()
                    .get();

            if (!issuedBookDoc.exists()) {
                throw new RuntimeException("Book not found in student's borrowed list.");
            }

            String bookName = issuedBookDoc.getString("bookName");

            Map<String, Object> bookData = new HashMap<>();
            bookData.put("id", bookId);
            bookData.put("name", bookName);

            firestore.collection("books").document(bookId).set(bookData);

            firestore.collection("studentbook")
                    .document(studentId)
                    .collection("issuedbook")
                    .document(bookId)
                    .delete();

        } catch (Exception e) {
            throw new RuntimeException("Failed to return book: " + e.getMessage(), e);
        }
    }

    public String getStudentBookDetails(String studentId) {
        try {
            // Parse studentId into components
            String programmeCode = studentId.substring(0, 1);
            String branchCode = studentId.substring(1, 2);
            String yearCode = studentId.substring(2, 4);

            String programme = switch (programmeCode.toLowerCase()) {
                case "b" -> "b-tech";
                case "a" -> "m-tech";
                case "c" -> "phd";
                default -> throw new RuntimeException("Invalid programme code");
            };

            String branch = switch (branchCode) {
                case "1" -> "CSE";
                case "2" -> "ETC";
                case "3" -> "EEE";
                case "4" -> "IT";
                case "5" -> "CE";
                default -> throw new RuntimeException("Invalid branch code");
            };

            String year = "20" + yearCode;

            // 🔍 Use the provided method to fetch student profile (including name and grade)
            Map<String, Object> studentData = studentSearchService.searchStudentById(branch, programme, year, studentId);

            String studentName = studentData.getOrDefault("name", studentId).toString();
            String grade = studentData.getOrDefault("grade", "N/A").toString();

            // 🔖 Fetch issued books
            CollectionReference issuedBooksRef = firestore
                    .collection("studentbook")
                    .document(studentId)
                    .collection("issuedbook");

            ApiFuture<QuerySnapshot> future = issuedBooksRef.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            List<Map<String, Object>> issuedBooks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                issuedBooks.add(doc.getData());
            }

            // 📦 Build final response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("studentId", studentId);
            response.put("name", studentName);
            response.put("branch", branch);
            response.put("programme", programme);
            response.put("year", year);
            response.put("grade", grade);
            response.put("issuedBooks", issuedBooks);

            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get student book details: " + e.getMessage(), e);
        }
    }


}
