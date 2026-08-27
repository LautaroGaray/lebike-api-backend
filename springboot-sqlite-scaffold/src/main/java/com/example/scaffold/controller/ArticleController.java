package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.inventory.ArticleRequestDTO;
import com.example.scaffold.dto.inventory.ArticleResponseDTO;
import com.example.scaffold.service.inventory.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody ArticleRequestDTO request) {
        try {
            ArticleResponseDTO created = articleService.create(request);
            return ResponseEntity.ok(new ResponseData(created, true, "Article created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{articleId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long articleId,
                                             @RequestBody ArticleRequestDTO request) {
        try {
            ArticleResponseDTO updated = articleService.update(articleId, request);
            return ResponseEntity.ok(new ResponseData(updated, true, "Article updated successfully"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("Article not found".equals(message)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, message));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
        }
    }

    @DeleteMapping("/delete/{articleId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long articleId) {

        try {
            articleService.delete(articleId);
            return ResponseEntity.ok(new ResponseData(null, true, "Article deleted successfully"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("Article not found".equals(message)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, message));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
        }
    }
}


