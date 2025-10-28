package com.pultyn.spring_oauth.integration;

import com.pultyn.spring_oauth.enums.UserRole;
import com.pultyn.spring_oauth.model.Book;
import com.pultyn.spring_oauth.model.UserEntity;
import com.pultyn.spring_oauth.repository.BookRepository;
import com.pultyn.spring_oauth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class BookControllerTest {
    @Container
    static final MySQLContainer<?> db =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("test-db")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
        r.add("spring.datasource.driver-class-name", () ->
                "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepo;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    BookRepository bookRepo;

    private static final String USER_KEYCLOAK_ID = "keycloak-user-123";
    private static final String ADMIN_KEYCLOAK_ID = "keycloak-admin-123";

    private String asJson(Object o) throws JsonProcessingException {
        return mapper.writeValueAsString(o);
    }

    @BeforeEach
    void purgeDb() {
        bookRepo.deleteAll();
        userRepo.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBook_success() throws Exception {
        Book book = bookRepo.save(Book.builder()
                .title("Clean Code").author("Robert C. Martin").build());
        mvc.perform(get("/api/books/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()));
    }

    @Test
    void getBooks_noSearch_success() throws Exception {
        Book book1 = bookRepo.save(Book.builder()
                .title("Clean Code").author("Robert C. Martin").build());
        Book book2 = bookRepo.save(Book.builder()
                .title("Dirty Code 1").author("Robert C. Martin").build());
        Book book3 = bookRepo.save(Book.builder()
                .title("Dirty Code 2").author("Robert C. Martin").build());
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getBooks_searchString_success() throws Exception {
        Book book1 = bookRepo.save(Book.builder()
                .title("Clean Code").author("Robert C. Martin").build());
        Book book2 = bookRepo.save(Book.builder()
                .title("Dirty Code 1").author("Robert C. Martin").build());
        Book book3 = bookRepo.save(Book.builder()
                .title("Dirty Code 2").author("Robert C. Martin").build());
        mvc.perform(get("/api/books?searchString=Dirty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createBook_admin_success() throws Exception {
        userRepo.save(UserEntity.builder()
                .email("admin@example.com")
                        .keycloakId(ADMIN_KEYCLOAK_ID)
                .build());

        String body = """
            { "title":"Clean Code", "author":"Robert C. Martin" }
        """;

        mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_BOOK_ADMIN"))
                                .jwt(token -> token
                                        .subject(ADMIN_KEYCLOAK_ID)
                                        .claim("preferred_username", "admin@example.com")
                                )
                        )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert C. Martin"));
    }

    @Test
    void createBook_user_forbidden() throws Exception {
        mvc.perform(post("/api/books")
                        .contentType("application/json")
                        .content("{\"title\":\"X\",\"author\":\"Y\"}")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_BOOK_USER"))
                                .jwt(token -> token
                                        .subject(USER_KEYCLOAK_ID)
                                        .claim("preferred_username", "user@example.com")
                                )
                        )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBook_admin_success() throws Exception {
        Book book = bookRepo.save(Book.builder()
                .title("Old").author("Some One").build());

        UserEntity admin = userRepo.save(UserEntity.builder()
                .email("admin@example.com")
                .password("{noop}pw")
                .role(UserRole.ADMIN)
                .build());
        String token = jwtService.generateToken(admin);

        String body = """
        { "title":"New Title", "author":"New Author" }
        """;

        mvc.perform(patch("/api/books/{id}", book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.author").value("New Author"));
    }

    @Test
    void updateBook_notFound_returns404() throws Exception {
        UserEntity admin = userRepo.save(UserEntity.builder()
                .email("admin@example.com")
                .password("{noop}pw")
                .role(UserRole.ADMIN)
                .build());
        String token = jwtService.generateToken(admin);

        mvc.perform(patch("/api/books/{id}", 999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"X\",\"author\":\"Y\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBook_user_forbidden() throws Exception {
        UserEntity user = userRepo.save(UserEntity.builder()
                .email("user@example.com")
                .password("{noop}pw")
                .role(UserRole.USER)
                .build());
        String token = jwtService.generateToken(user);

        Book book = bookRepo.save(Book.builder()
                .title("T").author("A").build());

        mvc.perform(patch("/api/books/{id}", book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"X\",\"author\":\"Y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteBook_success() throws Exception {
        UserEntity admin = userRepo.save(UserEntity.builder()
                .email("admin@example.com")
                .password("{noop}dummy")
                .role(UserRole.ADMIN)
                .build());

        Book book = bookRepo.save(Book.builder()
                .title("Clean Code").author("Robert C. Martin").build());
        String token = jwtService.generateToken(admin);

        mvc.perform(delete("/api/books/{id}", book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_user() throws Exception {
        UserEntity user = userRepo.save(UserEntity.builder()
                .email("user@example.com")
                .password("{noop}pw")
                .role(UserRole.USER)
                .build());

        Book book = bookRepo.save(Book.builder()
                .title("DDD").author("Eric Evans").build());

        String token = jwtService.generateToken(user);

        mvc.perform(delete("/api/books/{id}", book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}