package az.fitnest.identity.criteria;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCriteria {

    private Integer page;
    private String sortBy;

    public Integer getPage() {
        return page != null && page > 0 ? page - 1 : 0;
    }

    public enum SortDirection {
        ASC, DESC
    }
}
