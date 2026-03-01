package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldIssue {
    private String field;
    private String issue;
}
