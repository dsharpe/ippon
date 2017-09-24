package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MagazineApplication {

  @RequestMapping(value = "/new")
  public String available() {
    return "What is an Ippon?";
  }

  @RequestMapping(value = "/old")
  public String checkedOut() {
    return "Mainframes are Cool";
  }

  public static void main(String[] args) {
    SpringApplication.run(MagazineApplication.class, args);
  }
}
