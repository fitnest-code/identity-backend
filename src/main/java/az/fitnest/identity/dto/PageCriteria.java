package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

public record PageCriteria(Integer page, String sortBy) {
    public Integer getPage() {
        return page != null && page > 0 ? page - 1 : 0;
    }
    public enum SortDirection {
        ASC, DESC
    }
}
