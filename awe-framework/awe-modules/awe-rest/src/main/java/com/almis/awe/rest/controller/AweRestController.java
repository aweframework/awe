package com.almis.awe.rest.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;

/**
 * REST API awe-rest  for launching queries and maintains
 */
@RestController
@RequestMapping("/api")
@Slf4j
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
  public AweRestController(QueryService queryService, MaintainService maintainService, AweRequest request,
                           JWTTokenService jwtTokenService, ModelMapper modelMapper, ObjectMapper objectMapper) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.request = request;
    this.jwtTokenService = jwtTokenService;
    this.modelMapper = modelMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * Authenticate service. Generate JWT Token for authentication
   *
   * @param loginRequest        Login request
   * @param httpServletResponse response
   * @return LoginResponse with jwt token info
   */
  @PostMapping("/authenticate")
  @Tag(name = "Authentication API", description = "Authentication service")
  @Operation(summary = "Authentication service.",
    description = "Provides a JWT token to be sent as http header in services that require authentication.",
    tags = "Authentication API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Authorized OK", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
  public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
    Assert.notNull(loginRequest.getUsername());
    Assert.notNull(loginRequest.getPassword());
    // Decode http response authorization header
    String authorizationHeader = httpServletResponse.getHeader(jwtTokenService.getAuthorizationHeader());
    String token = jwtTokenService.extractToken(authorizationHeader);
    DecodedJWT decodedJWT = jwtTokenService.verifyToken(token);

    LoginResponse loginResponse = new LoginResponse();
    loginResponse.setUsername(loginRequest.getUsername());
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
  @Tag(name = "Protected API", description = "Protected admin interface to launch AWE services")
  @Operation(summary = "Query service.", description = "Launch a query given query id and parameters.",
    security = @SecurityRequirement(name = "bearerAuth"),
    tags = "Protected API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Query launched OK", content = @Content(schema = @Schema(implementation = AweRestResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request. Query not defined"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
  public ResponseEntity<AweRestResponse> launchQuery(@Parameter(description = "Query ID", required = true) @PathVariable String queryId,
                                                     @Parameter(description = "Query parameters") @RequestBody(required = false) RequestParameter parameters) throws AWException {
    setParameters(Optional.ofNullable(parameters).map(RequestParameter::getParameters).orElse(null));
    AweRestResponse aweRestResponse = convertServiceDataToDto(queryService.launchPrivateQuery(queryId));
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
  @Tag(name = "Public API", description = "Public admin interface to launch AWE services")
  @Operation(summary = "Public Query service.", description = "Launch a public query given query id and parameters.",
    tags = "Public API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Query launched OK", content = @Content(schema = @Schema(implementation = AweRestResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request. Query not defined"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
  public ResponseEntity<AweRestResponse> launchPublicQuery(@Parameter(description = "Query ID", required = true) @PathVariable String queryId, @Parameter(description = "Query parameters") @RequestBody(required = false) RequestParameter parameters) throws AWException {
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
  @Tag(name = "Protected API", description = "Protected admin interface to launch AWE services")
  @Operation(summary = "Maintain service.", description = "Launch a maintain given maintain id and parameters.",
    security = @SecurityRequirement(name = "bearerAuth"),
    tags = "Protected API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Maintain launched OK", content = @Content(schema = @Schema(implementation = AweRestResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request. Maintain not defined"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
  public ResponseEntity<AweRestResponse> launchMaintain(@PathVariable String maintainId, @RequestBody(required = false) RequestParameter parameters) throws AWException {
    setParameters(Optional.ofNullable(parameters).map(RequestParameter::getParameters).orElse(null));
    AweRestResponse aweRestResponse = convertServiceDataToDto(maintainService.launchMaintain(maintainId));
    // Clear context
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(aweRestResponse);
  }

  /**
   * Launch a maintain
   *
   * @param maintainId Maintain id
   * @param parameters JSON parameters
   * @return JSON response
   * @throws AWException the AWE exception
   */
  @PostMapping({"/maintain/async/{maintainId}"})
  @Tag(name = "Protected API", description = "Protected admin interface to launch AWE services asynchronously")
  @Operation(summary = "Maintain service.", description = "Launch a maintain given maintain id and parameters, and launch a callback function on maintain end",
    security = @SecurityRequirement(name = "bearerAuth"),
    tags = "Protected API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Maintain launched OK", content = @Content(schema = @Schema(implementation = AweRestResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request. Maintain not defined"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
  public Mono<AweRestResponse> launchMaintainMono(@PathVariable String maintainId, @RequestBody(required = false) RequestParameter parameters) throws AWException {
    setParameters(Optional.ofNullable(parameters).map(RequestParameter::getParameters).orElse(null));
    AweRestResponse aweRestResponse = convertServiceDataToDto(maintainService.launchMaintain(maintainId));
    // Clear context
    SecurityContextHolder.clearContext();
    return Mono.just(aweRestResponse);
  }

  /**
   * Launch a public maintain
   *
   * @param maintainId Maintain id
   * @param parameters JSON parameters
   * @return JSON response
   * @throws AWException AWE exception
   */
  @PostMapping({"/public/maintain/{maintainId}"})
  @Tag(name = "Public API", description = "Public admin interface to launch AWE services")
  @Operation(summary = "Maintain service.", description = "Launch a public maintain given maintain id and parameters.",
    tags = "Public API")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success. Maintain launched OK", content = @Content(schema = @Schema(implementation = AweRestResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad request. Maintain not defined"),
    @ApiResponse(responseCode = "401", description = "Unauthorized. User not found or invalid credentials"),
    @ApiResponse(responseCode = "500", description = "Internal error")})
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
  private void setParameters(Map<String, Object> parameters) {
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
