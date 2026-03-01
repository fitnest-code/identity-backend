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
public class UpdateLegalDocumentRequest {

    private String version;

    private String language;

    private String content;
}
