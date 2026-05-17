package backend.academy.linktracker.bot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserContext {
    private UserState state;
    private String pendingUrl;

    public UserContext(UserState state, String pendingUrl) {
        this.state = state;
        this.pendingUrl = pendingUrl;
    }
}
