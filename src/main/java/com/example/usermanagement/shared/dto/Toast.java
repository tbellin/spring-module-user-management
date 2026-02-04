package com.example.usermanagement.shared.dto;

/**
 * Toast notification DTO for flash messages.
 * <p>
 * Used with RedirectAttributes to display temporary success/error
 * notifications after form submissions.
 *
 * @param type    the Bootstrap alert class: "success" or "danger"
 * @param title   the notification title (e.g., "Success", "Error")
 * @param message the notification message to display
 */
public record Toast(String type, String title, String message) {

    /**
     * Convenience constructor that derives title from type.
     *
     * @param type    "success" or "danger"
     * @param message the notification message
     */
    public Toast(String type, String message) {
        this(type, type.equals("success") ? "Success" : "Error", message);
    }
}
