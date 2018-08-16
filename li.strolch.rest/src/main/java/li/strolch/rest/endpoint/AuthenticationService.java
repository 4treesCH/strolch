/*
 * Copyright 2015 Robert von Burg <eitch@eitchnet.ch>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.rest.endpoint;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.gson.*;
import li.strolch.exception.StrolchException;
import li.strolch.privilege.base.AccessDeniedException;
import li.strolch.privilege.base.InvalidCredentialsException;
import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.IPrivilege;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.privilege.model.Usage;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.StrolchRestfulConstants;
import li.strolch.rest.StrolchSessionHandler;
import li.strolch.rest.helper.ResponseUtil;
import li.strolch.runtime.privilege.PrivilegeHandler;
import li.strolch.utils.helper.ExceptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@Path("strolch/authentication")
public class AuthenticationService {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response authenticate(@Context HttpServletRequest request, @Context HttpHeaders headers, String data) {

		JsonObject login = new JsonParser().parse(data).getAsJsonObject();
		JsonObject loginResult = new JsonObject();

		try {

			StringBuilder sb = new StringBuilder();
			JsonElement usernameE = login.get("username");
			if (usernameE == null || usernameE.getAsString().length() < 2) {
				sb.append("Username was not given or is too short!"); //$NON-NLS-1$
			}

			JsonElement passwordE = login.get("password");
			if (passwordE == null) {
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("Password was not given!"); //$NON-NLS-1$
			}

			char[] password = passwordE == null ?
					new char[] {} :
					new String(Base64.getDecoder().decode(passwordE.getAsString())).toCharArray();
			if (password.length < 3) {
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("Password not given or too short!"); //$NON-NLS-1$
			}

			if (sb.length() != 0) {
				logger.error("Authentication failed due to: " + sb.toString());
				loginResult.addProperty("msg",
						MessageFormat.format("Could not log in due to: {0}", sb.toString())); //$NON-NLS-2$
				return Response.status(Status.BAD_REQUEST).entity(loginResult.toString()).build();
			}

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			Certificate certificate = sessionHandler.authenticate(usernameE.getAsString(), password);

			return getAuthenticationResponse(request, loginResult, certificate);

		} catch (InvalidCredentialsException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			loginResult.addProperty("msg", "Could not log in as the given credentials are invalid"); //$NON-NLS-1$
			return Response.status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (AccessDeniedException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return Response.status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (StrolchException | PrivilegeException e) {
			logger.error(e.getMessage(), e);
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return Response.status(Status.FORBIDDEN).entity(loginResult.toString()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			loginResult.addProperty("msg", MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return Response.serverError().entity(loginResult.toString()).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("sso")
	public Response authenticateSingleSignOn(@Context HttpServletRequest request, @Context HttpHeaders headers) {

		JsonObject loginResult = new JsonObject();

		try {

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			Certificate certificate = sessionHandler.authenticateSingleSignOn(request.getUserPrincipal());

			return getAuthenticationResponse(request, loginResult, certificate);

		} catch (InvalidCredentialsException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			loginResult.addProperty("msg", "Could not log in as the given credentials are invalid"); //$NON-NLS-1$
			return Response.status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (AccessDeniedException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return Response.status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (StrolchException | PrivilegeException e) {
			logger.error(e.getMessage(), e);
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return Response.status(Status.FORBIDDEN).entity(loginResult.toString()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			loginResult.addProperty("msg", MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return Response.serverError().entity(loginResult.toString()).build();
		}
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response invalidateSession(@PathParam("authToken") String authToken) {

		JsonObject logoutResult = new JsonObject();

		try {

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			Certificate certificate = sessionHandler.validate(authToken);
			sessionHandler.invalidate(certificate);

			logoutResult.addProperty("username", certificate.getUsername());
			logoutResult.addProperty("authToken", authToken);
			logoutResult.addProperty("msg", //$NON-NLS-1$
					MessageFormat.format("{0} has been logged out.", certificate.getUsername()));
			return Response.ok().entity(logoutResult.toString()).build();

		} catch (StrolchException | PrivilegeException e) {
			logger.error("Failed to invalidate session due to: " + e.getMessage());
			logoutResult.addProperty("msg",
					MessageFormat.format("Could not logout due to: {0}", e.getMessage())); //$NON-NLS-2$
			return Response.status(Status.UNAUTHORIZED).entity(logoutResult.toString()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			logoutResult
					.addProperty("msg", MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return Response.serverError().entity(logoutResult.toString()).build();
		}
	}

	@HEAD
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response validateSession(@PathParam("authToken") String authToken) {

		try {

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			sessionHandler.validate(authToken);

			return Response.ok().build();

		} catch (StrolchException | PrivilegeException e) {
			logger.error("Session validation failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}", e.getMessage()));
			String json = new Gson().toJson(root);
			return Response.status(Status.UNAUTHORIZED).entity(json).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}: {1}", e.getClass().getName(), msg));
			String json = new Gson().toJson(root);
			return Response.serverError().entity(json).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("challenge")
	public Response initiateChallenge(String data) {

		try {
			JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
			String username = jsonObject.get("username").getAsString();
			String usage = jsonObject.get("usage").getAsString();

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			sessionHandler.initiateChallengeFor(Usage.byValue(usage), username);

			return ResponseUtil.toResponse();

		} catch (PrivilegeException e) {
			logger.error("Challenge initialization failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", ExceptionHelper.getExceptionMessage(e));
			String json = new Gson().toJson(root);
			return Response.status(Status.UNAUTHORIZED).entity(json).build();
		}
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("challenge")
	public Response validateChallenge(@Context HttpServletRequest request, String data) {

		try {

			JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
			String username = jsonObject.get("username").getAsString();
			String challenge = jsonObject.get("challenge").getAsString();

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getSessionHandler();
			Certificate certificate = sessionHandler.validateChallenge(username, challenge);

			jsonObject = new JsonObject();
			jsonObject.addProperty("authToken", certificate.getAuthToken());

			boolean secureCookie = RestfulStrolchComponent.getInstance().isSecureCookie();
			if (secureCookie && !request.getScheme().equals("https")) {
				String msg = "Authorization cookie is secure, but connection is not secure! Cookie won't be passed to client!";
				logger.warn(msg);
			}

			NewCookie cookie = new NewCookie(StrolchRestfulConstants.STROLCH_AUTHORIZATION, certificate.getAuthToken(),
					"/", null, "Authorization header", (int) TimeUnit.DAYS.toSeconds(1), secureCookie);

			return Response.ok().entity(jsonObject.toString())//
					.header(HttpHeaders.AUTHORIZATION, certificate.getAuthToken()).cookie(cookie).build();

		} catch (PrivilegeException e) {
			logger.error("Challenge validation failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", ExceptionHelper.getExceptionMessage(e));
			String json = new Gson().toJson(root);
			return Response.status(Status.UNAUTHORIZED).entity(json).build();
		}
	}

	private Response getAuthenticationResponse(HttpServletRequest request, JsonObject loginResult,
			Certificate certificate) {

		PrivilegeHandler privilegeHandler = RestfulStrolchComponent.getInstance().getContainer().getPrivilegeHandler();
		PrivilegeContext privilegeContext = privilegeHandler.validate(certificate);
		loginResult.addProperty("sessionId", certificate.getSessionId());
		loginResult.addProperty("authToken", certificate.getAuthToken());
		loginResult.addProperty("username", certificate.getUsername());
		loginResult.addProperty("firstname", certificate.getFirstname());
		loginResult.addProperty("lastname", certificate.getLastname());
		loginResult.addProperty("locale", certificate.getLocale().toString());

		if (!certificate.getPropertyMap().isEmpty()) {
			JsonObject propObj = new JsonObject();
			loginResult.add("properties", propObj);
			for (String propKey : certificate.getPropertyMap().keySet()) {
				propObj.addProperty(propKey, certificate.getPropertyMap().get(propKey));
			}
		}

		if (!certificate.getUserRoles().isEmpty()) {
			JsonArray rolesArr = new JsonArray();
			loginResult.add("roles", rolesArr);
			for (String role : certificate.getUserRoles()) {
				rolesArr.add(new JsonPrimitive(role));
			}
		}

		if (!privilegeContext.getPrivilegeNames().isEmpty()) {
			JsonArray privArr = new JsonArray();
			loginResult.add("privileges", privArr);

			for (String name : privilegeContext.getPrivilegeNames()) {
				IPrivilege privilege = privilegeContext.getPrivilege(name);

				JsonObject privObj = new JsonObject();
				privArr.add(privObj);

				privObj.addProperty("name", name);
				privObj.addProperty("allAllowed", privilege.isAllAllowed());

				Set<String> allowSet = privilege.getAllowList();
				if (!allowSet.isEmpty()) {
					JsonArray allowArr = new JsonArray();
					privObj.add("allowList", allowArr);
					for (String allow : allowSet) {
						allowArr.add(new JsonPrimitive(allow));
					}
				}
			}
		}

		boolean secureCookie = RestfulStrolchComponent.getInstance().isSecureCookie();
		int cookieMaxAge = RestfulStrolchComponent.getInstance().getCookieMaxAge();
		if (secureCookie && !request.getScheme().equals("https")) {
			logger.warn(
					"Authorization cookie is secure, but connection is not secure! Cookie won't be passed to client!");
		}
		NewCookie cookie = new NewCookie(StrolchRestfulConstants.STROLCH_AUTHORIZATION, certificate.getAuthToken(), "/",
				null, "Authorization header", cookieMaxAge, secureCookie);

		return Response.ok().entity(loginResult.toString())//
				.header(HttpHeaders.AUTHORIZATION, certificate.getAuthToken()).cookie(cookie).build();
	}
}
