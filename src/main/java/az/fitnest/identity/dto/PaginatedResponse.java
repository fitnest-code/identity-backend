package az.fitnest.identity.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginatedResponse<T>(
    List<T> items,
    long total,
    int page,
    int pageSize
) {
    public static <T> PaginatedResponse<T> of(Page<T> pageResult) {
        return new PaginatedResponse<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getNumber() + 1,
                pageResult.getSize()
        );
    }
}
