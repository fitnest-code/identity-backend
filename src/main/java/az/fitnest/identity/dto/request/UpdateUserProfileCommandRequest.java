package az.fitnest.identity.dto.request;

public class UpdateUserProfileCommandRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;

    public UpdateUserProfileCommandRequest(String firstName, String lastName, String email, String mobile) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }
}
