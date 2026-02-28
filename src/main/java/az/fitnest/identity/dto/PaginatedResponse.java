package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PaginatedResponse<T> of(Page<T> pageResult) {
        int pageNumber = pageResult.getNumber() + 1;
        return PaginatedResponse.<T>builder()
                .items(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .page(pageNumber)
                .pageSize(pageResult.getSize())
                .build();
    }
}
