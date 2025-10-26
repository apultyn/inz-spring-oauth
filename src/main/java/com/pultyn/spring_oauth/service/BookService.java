package com.pultyn.spring_oauth.service;

import com.pultyn.spring_oauth.dto.BookDTO;
import com.pultyn.spring_oauth.exceptions.NotFoundException;
import com.pultyn.spring_oauth.model.Book;
import com.pultyn.spring_oauth.repository.BookRepository;
import com.pultyn.spring_oauth.request.CreateBookRequest;
import com.pultyn.spring_oauth.request.UpdateBookRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    public Book findBookById(Long bookId) throws NotFoundException {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    public List<BookDTO> getBooks(String searchString) {
        List<Book> books = bookRepository.searchBooks(searchString);
        return books.stream()
                .map(BookDTO::new)
                .collect(Collectors.toList());
    }

    public BookDTO createBook(CreateBookRequest bookRequest) {
        Book book = Book.builder()
                .title(bookRequest.getTitle())
                .author(bookRequest.getAuthor())
                .reviews(new ArrayList<>())
                .build();

        try {
            return new BookDTO(bookRepository.save(book));
        } catch (DataIntegrityViolationException ex) {
            throw new DataIntegrityViolationException("Book must have unique combination of title and author");
        }
    }

    public void deleteBook(Long bookId) throws NotFoundException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        bookRepository.delete(book);
    }

    public BookDTO updateBook(Long bookId, UpdateBookRequest bookRequest) throws NotFoundException {
        Book bookToUpdate = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        bookToUpdate.setAuthor(bookRequest.getAuthor());
        bookToUpdate.setTitle(bookRequest.getTitle());

        bookRepository.save(bookToUpdate);

        return new BookDTO(bookToUpdate);
    }
}
