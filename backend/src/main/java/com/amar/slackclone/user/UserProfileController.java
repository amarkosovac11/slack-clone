package com.amar.slackclone.user;

import com.amar.slackclone.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController @RequestMapping("/api/users")
public class UserProfileController {
    private final UserProfileService service;
    public UserProfileController(UserProfileService service){this.service=service;}
    @GetMapping("/me") public CurrentUserProfileResponse me(Authentication a){return service.me(a.getName());}
    @GetMapping("/{userId}") public UserSummaryResponse summary(@PathVariable Long userId,Authentication a){return service.summary(a.getName(),userId);}
    @PatchMapping("/me") public CurrentUserProfileResponse update(@Valid @RequestBody UpdateProfileRequest r,Authentication a){return service.update(a.getName(),r);}
    @PutMapping("/me/status") public CurrentUserProfileResponse status(@Valid @RequestBody UpdateStatusRequest r,Authentication a){return service.status(a.getName(),r);}
    @DeleteMapping("/me/status") public CurrentUserProfileResponse clearStatus(Authentication a){return service.clearStatus(a.getName());}
    @PostMapping(value="/me/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public CurrentUserProfileResponse avatar(@RequestPart("file") MultipartFile f,Authentication a)throws IOException{return service.avatar(a.getName(),f);}
    @DeleteMapping("/me/avatar") public CurrentUserProfileResponse removeAvatar(Authentication a)throws IOException{return service.removeAvatar(a.getName());}
    @PostMapping("/me/change-password") @ResponseStatus(HttpStatus.NO_CONTENT) public void password(@Valid @RequestBody ChangePasswordRequest r,Authentication a){service.changePassword(a.getName(),r);}
    @GetMapping("/avatars/{key}") public ResponseEntity<Resource> avatar(@PathVariable String key){Resource r=service.avatarResource(key);if(!r.exists())return ResponseEntity.notFound().build();return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CACHE_CONTROL,"public, max-age=86400").body(r);}
}
