package com.almis.awe.rest.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * REST API awe-rest  for launching queries and maintains
 */
@RestController
@RequestMapping("/api")
@Log4j2
@Tags({@Tag(name = "Authentication API", description = "Authentication service"),
        @Tag(name = "Public API", description = "Public admin interface to launch AWE services"),
        @Tag(name = "Protected API", description = "Protected admin interface to launch AWE services")})
public class AweRestController {

  // Constants
  public static final String AWE_REST = "[awe-rest] ";

  // Autowired services
  private final QueryService queryService;
  private final MaintainService maintainService;
  private final AweRequest request;
  private final JWTTokenService jwtTokenService;
  private final ModelMapper modelMapper;
  private final ObjectMapper objectMapper;

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param request         the request
   * @param jwtTokenService JWT Token service
   * @param modelMapper     the model mapper
   */
  @Autowired
  public AweRestController(QueryService queryService, MaintainService maintainService, AweRequest request, JWTTokenService jwtTokenService, ModelMapper modelMapper, ObjectMapper objectMapper) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.request = request;
    this.jwtTokenService = jwtTokenService;
    this.modelMapper = modelMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * Authenticate service. Generate JWT Token for authentication
   * @param username User name
   * @param password User password
   * @param httpServletResponse response
   * @return LoginResponse with jwt token info
   */
  @PostMapping("/authenticate")
  @ApiOperation(value = "Authentication service.",
          notes = "Provides a JWT token to be sent as http header in services that require authentication.",
          authorizations = {@Authorization(value = "")},
          tags = "Authentication API")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Success. Authorized OK", response = LoginResponse.class),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "Unauthorized. User not found or invalid credentials"),
          @ApiResponse(code = 500, message = "Internal error")})
  public ResponseEntity<LoginResponse> authenticate(@ApiParam(value = "User name to be authenticated", required = true) @RequestParam String username, @ApiParam(value = "Credentials of user", required = true) @RequestParam String password, HttpServletResponse httpServletResponse) {
    // Decode http response authorization header
    DecodedJWT decodedJWT = jwtTokenService.verifyToken(httpServletResponse.getHeader(jwtTokenService.getAuthorizationHeader()));

    LoginResponse loginResponse = new LoginResponse();
    loginResponse.setUsername(username);
    loginResponse.setToken(decodedJWT.getToken());
    loginResponse.setIssuer(decodedJWT.getIssuer());
    loginResponse.setExpiresAt(decodedJWT.getExpiresAt());

    return ResponseEntity.ok(loginResponse);
  }

  /**
   * Request a query
   *
   * @param queryId    Query id
   * @param parameters JSON parameters
   * @return JSON response
   * @throws AWException the AWE exception
   */
  @PostMapping({"/data/{queryId}"})
  @ApiOperation(value = "Query service.", notes = "Launch a query given query id and parameters.",
          authorizations = {@Authorization(value = "JWTToken")},
          tags = "Protected API")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Success. Query launched OK", response = AweRestResponse.class),
          @ApiResponse(code = 400, message = "Bad request. Query not defined"),
          @ApiResponse(code = 401, message = "Unauthorized. User not found or invalid credentials"),
          @ApiResponse(code = 500, message = "Internal error")})
  public ResponseEntity<AweRestResponse> launchQuery(@ApiParam(value = "Query ID", required = true) @PathVariable String queryId,
                                                     @ApiParam(value = "Query parameters") @RequestBody(required = false) RequestParameter parameters) throws AWException {
    setParameters(parameters);
    AweRestResponse aweRestResponse = convertServiceDataToDto(queryService.launchQuery(queryId));
    // Clear context
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(aweRestResponse);
  }

  /**
   * Launch a public query
   *
   * @param queryId    Query id
   * @param parameters JSON parameters
   * @return JSON response
   * @throws AWException the AWE exception
   */
  @PostMapping({"/public/data/{queryId}"})
  @ApiOperation(value = "Public Query service.", notes = "Launch a public query given query id and parameters.",
          authorizations = {@Authorization(value = "")},
          tags = "Public API")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Success. Query launched OK", response = AweRestResponse.class),
          @ApiResponse(code = 400, message = "Bad request. Query not defined"),
          @ApiResponse(code = 401, message = "Unauthorized. User not found or invalid credentials"),
          @ApiResponse(code = 500, message = "Internal error")})
  public ResponseEntity<AweRestResponse> launchPublicQuery(@ApiParam(value = "Query ID", required = true) @PathVariable String queryId, @ApiParam(value = "Query parameters") @RequestBody(required = false) RequestParameter parameters) throws AWException {
   return launchQuery(queryId, parameters);
  }

  /**
   * Launch a maintain
   *
   * @param maintainId Maintain id
   * @param parameters JSON parameters
   * @return JSON response
   * @throws AWException the AWE exception
   */
  @PostMapping({"/maintain/{maintainId}"})
  @ApiOperation(value = "Maintain service.", notes = "Launch a maintain given maintain id and parameters.",
          authorizations = {@Authorization(value = "JWTToken")},
          tags = "Protected API")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Success. Maintain launched OK", response = AweRestResponse.class),
          @ApiResponse(code = 400, message = "Bad request. Maintain not defined"),
          @ApiResponse(code = 401, message = "Unauthorized. User not found or invalid credentials"),
          @ApiResponse(code = 500, message = "Internal error")})
  public ResponseEntity<AweRestResponse> launchMaintain(@PathVariable String maintainId, @RequestBody(required = false) RequestParameter parameters) throws AWException {
    setParameters(parameters);
    AweRestResponse aweRestResponse = convertServiceDataToDto(maintainService.launchMaintain(maintainId));
    // Clear context
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(aweRestResponse);
  }

  /**
   * Launch a public maintain
   *
   * @param maintainId Maintain id
   * @param parameters JSON parameters
   * @throws AWException AWE exception
   * @return JSON response
   */
  @PostMapping({"/public/maintain/{maintainId}"})
  @ApiOperation(value = "Maintain service.", notes = "Launch a public maintain given maintain id and parameters.",
          authorizations = {@Authorization(value = "")},
          tags = "Public API")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Success. Maintain launched OK", response = AweRestResponse.class),
          @ApiResponse(code = 400, message = "Bad request. Maintain not defined"),
          @ApiResponse(code = 401, message = "Unauthorized. User not found or invalid credentials"),
          @ApiResponse(code = 500, message = "Internal error")})
  public ResponseEntity<AweRestResponse> launchPublicMaintain(@PathVariable String maintainId, @RequestBody(required = false) RequestParameter parameters) throws AWException {
    return launchMaintain(maintainId, parameters);
  }

  /**
   * Handle rest error
   *
   * @param exc Exception to handle
   * @return Rest response error
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public AweRestResponse handleException(Exception exc) {
    log.debug(AWE_REST + exc);
    // Retrieve exception
    AweRestResponse aweRestResponse = new AweRestResponse();
    aweRestResponse.setType(AnswerType.ERROR);
    aweRestResponse.setTitle(AWE_REST);
    aweRestResponse.setMessage(exc.getMessage());
    return aweRestResponse;
  }

  /**
   * Handle Query or Maintain not defined error
   *
   * @param exc Exception to handle
   * @return Response error
   */
  @ExceptionHandler(AWException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public AweRestResponse handleException(AWException exc) {
    log.debug(AWE_REST + exc);
    // Retrieve exception
    AweRestResponse aweRestResponse = new AweRestResponse();
    aweRestResponse.setType(AnswerType.ERROR);
    aweRestResponse.setTitle(AWE_REST);
    aweRestResponse.setMessage(exc.getMessage());
    return aweRestResponse;
  }

  /**
   * Handle authentication error
   *
   * @param exc Exception to handle
   * @return Response error
   */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public AweRestResponse handleAuthenticationException(Exception exc) {
    log.debug(AWE_REST + exc);
    // Retrieve exception
    AweRestResponse aweRestResponse = new AweRestResponse();
    aweRestResponse.setType(AnswerType.ERROR);
    aweRestResponse.setTitle(AWE_REST);
    aweRestResponse.setMessage("Not authorized. " + exc.getMessage());
    return aweRestResponse;
  }

  /**
   * Set parameters in request
   *
   * @param parameters Parameters
   */
  private void setParameters(RequestParameter parameters) {
    if (parameters != null) {
      request.setParameterList(objectMapper.valueToTree(parameters));
    }
  }

  /**
   * Map ServiceData to DTO rest response
   *
   * @param serviceData service data
   * @return AweRestDTO
   */
  private AweRestResponse convertServiceDataToDto(ServiceData serviceData) {
    return modelMapper.map(serviceData, AweRestResponse.class);
  }
}
