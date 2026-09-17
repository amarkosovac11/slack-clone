package com.amar.slackclone.search;
import com.amar.slackclone.search.dto.SearchPageResponse;import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/search") public class SearchController{
private final SearchService service;public SearchController(SearchService service){this.service=service;}
@GetMapping public SearchPageResponse search(@RequestParam String q,@RequestParam(defaultValue="ALL") SearchType type,@RequestParam(required=false)Long workspaceId,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(50)int size,Authentication authentication){return service.search(q,type,workspaceId,page,size,authentication.getName());}}
