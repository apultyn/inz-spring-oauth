package com.pultyn.spring_oauth.service;

import com.pultyn.spring_oauth.dto.ReviewDTO;
import com.pultyn.spring_oauth.exceptions.NotFoundException;
import com.pultyn.spring_oauth.model.Book;
import com.pultyn.spring_oauth.model.Review;
import com.pultyn.spring_oauth.model.UserEntity;
import com.pultyn.spring_oauth.repository.ReviewRepository;
import com.pultyn.spring_oauth.request.CreateReviewRequest;
import com.pultyn.spring_oauth.request.UpdateReviewRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private BookService bookService;

    public Review getReview(Long reviewId) throws NotFoundException {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
    }

    public List<ReviewDTO> getBookReviews(Long bookId) {
        List<Review> reviews = reviewRepository.findByBookId(bookId);

        return reviews.stream()
                .map(ReviewDTO::new)
                .collect(Collectors.toList());
    }

    public ReviewDTO createReview(CreateReviewRequest createReviewRequest, Jwt jwt)
            throws NotFoundException {
        UserEntity user =  userService.getOrCreateUser(jwt);
        Book book = bookService.findBookById(createReviewRequest.getBookId());

        Review review = Review.builder()
                .book(book)
                .user(user)
                .stars(createReviewRequest.getStars())
                .comment(createReviewRequest.getComment())
                .build();
        try {
            return new ReviewDTO(reviewRepository.save(review));
        } catch(DataIntegrityViolationException ex) {
            throw new DataIntegrityViolationException("User can write max 1 review per book");
        }
    }

    public ReviewDTO updateReview(
            Long reviewId,
            UpdateReviewRequest reviewRequest
    ) throws NotFoundException {
        Review reviewToUpdate = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        reviewToUpdate.setStars(reviewRequest.getStars());
        reviewToUpdate.setComment(reviewRequest.getComment());

        reviewRepository.save(reviewToUpdate);
        return new ReviewDTO(reviewToUpdate);
    }

    public void deleteReview(Long reviewId) throws NotFoundException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        reviewRepository.delete(review);
    }
}
