package com.pultyn.spring_oauth.controller;

import com.pultyn.spring_oauth.dto.BookDTO;
import com.pultyn.spring_oauth.exceptions.NotFoundException;
import com.pultyn.spring_oauth.model.Book;
import com.pultyn.spring_oauth.request.CreateBookRequest;
import com.pultyn.spring_oauth.request.UpdateBookRequest;
import com.pultyn.spring_oauth.service.BookService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping("/{bookId}")
    public ResponseEntity<?> getBook(@NotNull @PathVariable Long bookId) throws NotFoundException {
        Book book = bookService.findBookById(bookId);
        return ResponseEntity.ok(new BookDTO(book));
    }

    @GetMapping("")
    public ResponseEntity<?> getBooks(@RequestParam(defaultValue = "") String searchString) {
        List<BookDTO> books = bookService.getBooks(searchString);
        return ResponseEntity.ok(books);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('BOOK_ADMIN')")
    public ResponseEntity<?> createBook(@Valid @RequestBody CreateBookRequest bookRequest) {
        BookDTO book = bookService.createBook(bookRequest);
        return new ResponseEntity<BookDTO>(book, HttpStatus.CREATED);
    }

    @PatchMapping("/{bookId}")
    @PreAuthorize("hasRole('BOOK_ADMIN')")
    @Transactional
    public ResponseEntity<BookDTO> updateBook(
            @NotNull @PathVariable Long bookId,
            @Valid @RequestBody UpdateBookRequest bookRequest
    ) throws NotFoundException {
        BookDTO updatedBook = bookService.updateBook(bookId, bookRequest);
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('BOOK_ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteBook(@NotNull @PathVariable Long bookId) throws NotFoundException {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
