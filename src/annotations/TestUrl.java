package annotations;

@Controller
public class TestUrl {
    @Url("/hello")
    public void greeting() {}
}
