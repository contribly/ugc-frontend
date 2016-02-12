import com.restfb.types.User
import com.restfb.{DefaultFacebookClient, FacebookClient, Parameter}
import org.specs2.mutable._

class FacebookITSpec extends Specification {

  /*
  val facebookToken = ""

  val facebookClient: FacebookClient  = new DefaultFacebookClient(facebookToken, com.restfb.Version.VERSION_2_5)

  "can exchange Fackbook token for user profile" in {
    val user: User = facebookClient.fetchObject("me", classOf[User]);

    user.getId must not beNull

    user.getName must not beNull
  }

  "can obtain email address from token with email permission" in {
    val user: User = facebookClient.fetchObject("me", classOf[User], Parameter.`with`("fields", "id,name,email"))

    user.getEmail must not beNull
  }

  "can obtain first and last names but your have to ask for them" in {
    val user: User = facebookClient.fetchObject("me", classOf[User], Parameter.`with`("fields", "id,first_name,last_name"))

    println(user)

    user.getFirstName must not beNull

    user.getLastName must not beNull
  }
  */

}
