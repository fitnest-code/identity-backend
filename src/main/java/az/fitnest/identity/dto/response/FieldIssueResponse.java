package az.fitnest.identity.dto.response;

public class FieldIssueResponse {
    private final String field;
    private final String issue;

    public FieldIssueResponse(String field, String issue) {
        this.field = field;
        this.issue = issue;
    }

    public String getField() {
        return field;
    }

    public String getIssue() {
        return issue;
    }
}
