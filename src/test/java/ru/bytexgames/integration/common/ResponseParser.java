package ru.bytexgames.integration.common;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ResponseParser {


    // {ID=2, FIRSTNAME=First, LASTNAME=Last}, {ID=9, .....
    public List<User> checkAndParseUserList(final String s) {
        final List<User> result = new ArrayList<>();
        int cl = 0, p;

        assertThat("Invalid response", s.length(), greaterThan(1));
        assertThat("Invalid response, opening '[' missing", s.charAt(cl), equalTo('['));

        cl += 1;
        while (cl < s.length()) {
            if (s.charAt(cl) == '{') {
                final User user = new User();

                cl += 1;
                assertThat("Invalid response, ID is missing", s.substring(cl, cl+3), equalTo("ID="));
                cl += 3;
                p = s.indexOf(", FIRSTNAME=", cl);
                assertThat("Invalid response, ID couldn't be extracted", p, not(equalTo(-1)));
                try {
                    user.id = Integer.parseInt(s.substring(cl, p));
                } catch (NumberFormatException ex) {
                    assertFalse("Invalid response, ID is nmot a number", false);
                }
                cl = p;
                assertThat("Invalid response, FIRSTNAME is missing", s.substring(cl, cl+12), equalTo(", FIRSTNAME="));
                cl += 12;
                p = s.indexOf(", LASTNAME=", cl);
                assertThat("Invalid response, FIRSTNAME couldn't be extracted", p, not(equalTo(-1)));
                user.firstName = s.substring(cl, p);
                cl = p;
                assertThat("Invalid response, LASTNAME is missing", s.substring(cl, cl+11), equalTo(", LASTNAME="));
                cl += 11;
                p = s.indexOf("}", cl);
                assertThat("Invalid response, LASTNAME couldn't be extracted", p, not(equalTo(-1)));
                user.lastName = s.substring(cl, p);
                cl = p + 1;

                result.add(user);

                if (s.charAt(cl) == ',') cl += 2;
            } else {
                assertThat("Invalid response, closing ']' missing", s.charAt(cl), equalTo(']'));
                break;
            }
        }

        return result;
    }
}
