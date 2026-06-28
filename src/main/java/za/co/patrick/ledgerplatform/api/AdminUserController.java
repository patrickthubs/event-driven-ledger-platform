package za.co.patrick.ledgerplatform.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.patrick.ledgerplatform.application.AdminUserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController {

    private final AdminUserService adminUserService;

    AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    List<AdminUserResponse> listUsers() {
        return adminUserService.listUsers();
    }
}
