package com.library.book.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.library.book.application.service.BookService;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.Category;
import com.library.book.fixture.BookFixture;
import com.library.book.presentation.rest.request.CreateBookRequest;
import com.library.book.presentation.rest.request.UpdateBookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookController")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("removal")
class BookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookService bookService;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        var controller = new BookController(bookService);
        var messageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BookExceptionHandler())
                .setMessageConverters(messageConverter)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/books")
    class CreateBook {

        @Test
        @DisplayName("유효한 요청으로 도서를 생성하면 201을 반환한다")
        void returns_201_when_created() throws Exception {
            // given
            var book = BookFixture.available();
            when(bookService.createBook(any())).thenReturn(book);

            var request = new CreateBookRequest(
                    BookFixture.VALID_ISBN,
                    "Clean Code",
                    "Robert C. Martin",
                    "인사이트",
                    LocalDate.of(2013, 12, 24),
                    Category.TECHNOLOGY
            );

            // when & then
            mockMvc.perform(post("/api/v1/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.title").value(book.title()))
                    .andExpect(jsonPath("$.author").value(book.author()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/books/{id}")
    class GetBook {

        @Test
        @DisplayName("존재하는 ID로 조회하면 200을 반환한다")
        void returns_200_when_found() throws Exception {
            // given
            var book = BookFixture.available();
            when(bookService.findById(any(BookId.class)))
                    .thenReturn(Optional.of(book));

            // when & then
            mockMvc.perform(get("/api/v1/books/{id}", book.id().value()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(book.id().toString()))
                    .andExpect(jsonPath("$.title").value(book.title()));
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 404를 반환한다")
        void returns_404_when_not_found() throws Exception {
            // given
            var id = BookId.generate();
            when(bookService.findById(any(BookId.class)))
                    .thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/v1/books/{id}", id.value()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/books")
    class GetBooks {

        @Test
        @DisplayName("전체 도서 목록을 반환한다")
        void returns_all_books() throws Exception {
            // given
            var books = BookFixture.listOf(3);
            when(bookService.search(any())).thenReturn(books);

            // when & then
            mockMvc.perform(get("/api/v1/books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andExpect(jsonPath("$.books").isArray())
                    .andExpect(jsonPath("$.books.length()").value(3));
        }

        @Test
        @DisplayName("제목으로 검색한 결과를 반환한다")
        void returns_books_by_title() throws Exception {
            // given
            var books = BookFixture.listOf(2);
            when(bookService.search(any())).thenReturn(books);

            // when & then
            mockMvc.perform(get("/api/v1/books")
                            .param("title", "Clean"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/books/{id}")
    class UpdateBook {

        @Test
        @DisplayName("도서 정보를 수정하면 200을 반환한다")
        void returns_200_when_updated() throws Exception {
            // given
            var book = BookFixture.withTitle("Updated Title");
            when(bookService.updateBook(any(), any())).thenReturn(book);

            var request = new UpdateBookRequest(
                    "Updated Title",
                    "Updated Author",
                    "인사이트",
                    LocalDate.of(2013, 12, 24),
                    Category.TECHNOLOGY
            );

            // when & then
            mockMvc.perform(put("/api/v1/books/{id}", book.id().value())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/books/{id}")
    class DeleteBook {

        @Test
        @DisplayName("도서를 삭제하면 204를 반환한다")
        void returns_204_when_deleted() throws Exception {
            // given
            var id = BookId.generate();

            // when & then
            mockMvc.perform(delete("/api/v1/books/{id}", id.value()))
                    .andExpect(status().isNoContent());

            verify(bookService).deleteBook(any(BookId.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/books/isbn/{isbn}")
    class GetBookByIsbn {

        @Test
        @DisplayName("ISBN으로 도서를 조회하면 200을 반환한다")
        void returns_200_when_found() throws Exception {
            // given
            var book = BookFixture.available();
            when(bookService.findByIsbn(any())).thenReturn(Optional.of(book));

            // when & then
            mockMvc.perform(get("/api/v1/books/isbn/{isbn}", BookFixture.VALID_ISBN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isbn").value(book.isbn().value()));
        }

        @Test
        @DisplayName("존재하지 않는 ISBN으로 조회하면 404를 반환한다")
        void returns_404_when_not_found() throws Exception {
            // given
            when(bookService.findByIsbn(any())).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/v1/books/isbn/{isbn}", BookFixture.VALID_ISBN))
                    .andExpect(status().isNotFound());
        }
    }
}
