package com.example.usermanagement.auth.internal;

import com.example.usermanagement.shared.dto.Toast;
import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.DuplicateResourceException;
import com.example.usermanagement.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf controller for admin user management pages.
 * <p>
 * Serves the admin user list page with search, filter, sort, and pagination,
 * and the create user (invite) form page.
 * <p>
 * Placed in {@code auth.internal} alongside {@link AdminController} to access
 * both {@link UserService} and {@link AdminInviteService} without violating
 * module boundaries.
 */
@Controller
public class AdminWebController {

    private final UserService userService;
    private final AdminInviteService adminInviteService;

    public AdminWebController(UserService userService, AdminInviteService adminInviteService) {
        this.userService = userService;
        this.adminInviteService = adminInviteService;
    }

    /**
     * Displays the admin user list page with search, filter, sort, and pagination.
     *
     * @param search    partial text to match against email, first name, or last name
     * @param role      role name to filter by (e.g., "ROLE_ADMIN")
     * @param status    account status: "active" or "disabled"
     * @param sort      the field to sort by (default: createdAt)
     * @param direction the sort direction: "asc" or "desc" (default: desc)
     * @param pageable  pagination parameters (default: 10 per page)
     * @param model     the Thymeleaf model
     * @return the admin/users template name
     */
    @GetMapping("/admin/users")
    public String listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        // Build pageable from sort/direction params
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.Direction.fromString(direction),
            sort
        );

        Page<UserDto> users = userService.findUsers(search, role, status, sortedPageable);

        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);

        return "admin/users";
    }

    /**
     * Displays the create user (invite) form.
     *
     * @return the admin/user-new template name
     */
    @GetMapping("/admin/users/new")
    public String showCreateUserForm() {
        return "admin/user-new";
    }

    /**
     * Handles create user (invite) form submission.
     * <p>
     * On success, redirects to the user list with a success toast.
     * On duplicate email, redirects back to the form with an error toast.
     *
     * @param email              the email address of the user to invite
     * @param role               the role to assign
     * @param redirectAttributes for flash messages
     * @return redirect URL
     */
    @PostMapping("/admin/users/new")
    public String createUser(
            @RequestParam String email,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        try {
            adminInviteService.inviteUser(email, role);
            redirectAttributes.addFlashAttribute("toast",
                new Toast("success", "Invitation sent to " + email));
            return "redirect:/admin/users";
        } catch (DuplicateResourceException e) {
            redirectAttributes.addFlashAttribute("toast",
                new Toast("danger", "A user with this email already exists."));
            return "redirect:/admin/users/new";
        }
    }
}
