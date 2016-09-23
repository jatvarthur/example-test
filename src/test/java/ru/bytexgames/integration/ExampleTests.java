package ru.bytexgames.integration;

import org.junit.Test;
import ru.bytexgames.integration.common.ResponseParser;
import ru.bytexgames.integration.common.User;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ExampleTests {

	private static final String ENDPOINT = "http://localhost:28080/rs/users/";

    private static int NTHREADS = 10;
    private static int NUSERS_MAX = 20;

    private static int DELAY_MIN = 3000;
    private static int DELAY_MAX = 10000;

    private static final ResponseParser parser = new ResponseParser();


    private String query(final String method, final String id, final String params) {
        String result = "";
        try {
            URL url = new URL(ENDPOINT + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);

            if (params != null) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
                OutputStream output = connection.getOutputStream();
                output.write(params.getBytes("utf-8"));
                output.close();
            }

            InputStream is = connection.getInputStream();
            Scanner s = new Scanner(is).useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            is.close();
            connection.disconnect();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    private synchronized List<User> getUsers() {
        final String response = query("GET", "", null);

		return parser.checkAndParseUserList(response);
	}

    private synchronized List<User> createUser(User user) {
        try {
            final String params = String.format("firstName=%s&lastName=%s",
                    URLEncoder.encode(user.firstName, "utf-8"),
                    URLEncoder.encode(user.lastName, "utf-8"));

            final String response = query("POST", "", params);

            return parser.checkAndParseUserList(response);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized List<User> updateUser(User user) {
        try {
            final String params = String.format("firstName=%s&lastName=%s",
                    URLEncoder.encode(user.firstName, "utf-8"),
                    URLEncoder.encode(user.lastName, "utf-8"));

            final String response = query("PUT", user.id.toString(), params);

            return parser.checkAndParseUserList(response);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
	}

    private synchronized void deleteUser(User user) {
        final String response = query("DELETE", user.id.toString(), null);
	}

	@Test
	public void testCreateSingle() {
		final User test = new User(0, "User  , First   Name", "User  , Last    Name");

		// create and check what returned
		final List<User> created = createUser(test);
		assertThat(created.size(), equalTo(1));
		assertThat(created.get(0).firstName, equalTo(test.firstName));
		assertThat(created.get(0).lastName, equalTo(test.lastName));

		// check if the user present in output of all users
		final List<User> afterCreation = getUsers();
		Optional<User> found = afterCreation
				.stream()
				.filter(u -> Objects.equals(u.id, created.get(0).id))
				.findFirst();

		assertTrue(found.isPresent());
		assertThat(found.get().firstName, equalTo(test.firstName));
		assertThat(found.get().lastName, equalTo(test.lastName));

		deleteUser(created.get(0));

		// check if the user present in output of all users
		final List<User> afterDeletion = getUsers();
		assertThat(afterDeletion.size(), equalTo(0));

		/* -or
		found = afterDeletion
				.stream()
				.filter(u -> Objects.equals(u.id, created.get(0).id))
				.findFirst();

		assertFalse(found.isPresent());
		*/

	}

	@Test
	public void testCreateMultiple() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
        final Runnable task = () -> {
            final String threadName = Thread.currentThread().getName();
            final List<User> users  = new ArrayList<>();
            int count = (int)(Math.random() * (NUSERS_MAX - 1)) + 1;
            int delay = (int)(Math.random() * (DELAY_MAX - DELAY_MIN)) + DELAY_MIN;

            for (int i = 0; i < count; ++i) {
                final User user = new User(0, threadName+"_first_"+i, threadName+"_last_"+i);
                final List<User> created = createUser(user);
                assertThat(created.size(), equalTo(1));
                assertThat(created.get(0).firstName, equalTo(user.firstName));
                assertThat(created.get(0).lastName, equalTo(user.lastName));

                users.add(created.get(0));
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) { }

            for (final User user : users)
                deleteUser(user);

            System.out.printf("Completed %s\n", threadName);
        };

        for (int i = 0; i < NTHREADS; ++i) {
            executor.execute(task);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        final List<User> afterDeletion = getUsers();
        assertThat(afterDeletion.size(), equalTo(0));
    }

}
