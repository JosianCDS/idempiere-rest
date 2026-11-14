# com.trekglobal.idempiere.rest.api

- Copyright: Contributors; 2026 https://www.casadelsoftware.com (fork enhancements).
- Repository: https://github.com/JosianCDS/idempiere-rest
- License: GPL 2 or later, as stated in the source headers.

## Description

REST integration plugin for iDempiere 12, based on the upstream REST API. API v1 exposes business records, windows, processes, reports and administrative resources. This fork adds progressive API v2 authentication and TOTP self-service using the native iDempiere MFA mechanisms.

## Contributors

- Trek Global Corporation and upstream contributors, as credited in the source headers.
- Casa del Software (fork enhancements, 2026).

## Components

- iDempiere Plugin [com.trekglobal.idempiere.rest.api](com.trekglobal.idempiere.rest.api)
- iDempiere Unit Test Plugin [com.trekglobal.idempiere.rest.api.test](com.trekglobal.idempiere.rest.api.test)

The test bundle has its own activator; it is a test plugin rather than a fragment.

## Prerequisites

- Java 17, including commands `java` and `javac`.
- iDempiere 12, with native MFA models and a TOTP method configured for MFA operations.
- Resolved OSGi dependencies from the main manifest: Jackson 2.15.4, Jersey 2.34, HK2, Jakarta REST API 2.1.6, Jakarta Annotation API, Gson 2.9, GlassFish PFL, Apache Commons Math 3.6.1 and the declared servlet/imported packages.
- Applicable REST database migrations.

## Features/Documentation

### Source Structure

All files under the main and test source directories:

```text
├── com.trekglobal.idempiere.rest.api
│   └── src
│       └── com
│           └── trekglobal
│               └── idempiere
│                   └── rest
│                       └── api
│                           ├── Activator.java
│                           ├── ResourceExtension.java
│                           ├── json
│                           │   ├── BinaryTypeConverter.java
│                           │   ├── DateTypeConverter.java
│                           │   ├── DefaultGridTabSerializer.java
│                           │   ├── DefaultPOSerializer.java
│                           │   ├── IDempiereRestException.java
│                           │   ├── IGridTabSerializer.java
│                           │   ├── IGridTabSerializerFactory.java
│                           │   ├── IPOSerializer.java
│                           │   ├── IPOSerializerFactory.java
│                           │   ├── ITypeConverter.java
│                           │   ├── ImageTypeConverter.java
│                           │   ├── JSONTypeConverter.java
│                           │   ├── LocationTypeConverter.java
│                           │   ├── LookupTypeConverter.java
│                           │   ├── ModelHelper.java
│                           │   ├── NumericTypeConverter.java
│                           │   ├── POParser.java
│                           │   ├── Process.java
│                           │   ├── QueryOperators.java
│                           │   ├── ResponseUtils.java
│                           │   ├── RestUtils.java
│                           │   ├── TypeConverterUtils.java
│                           │   ├── YesNoTypeConverter.java
│                           │   ├── expand
│                           │   │   ├── ExpandParser.java
│                           │   │   └── ExpandUtils.java
│                           │   └── filter
│                           │       ├── ConvertedQuery.java
│                           │       ├── DefaultQueryConverter.java
│                           │       ├── IQueryConverter.java
│                           │       ├── IQueryConverterFactory.java
│                           │       └── ODataUtils.java
│                           ├── model
│                           │   ├── CalloutRestOIDCService.java
│                           │   ├── CalloutRestView.java
│                           │   ├── I_REST_AuthToken.java
│                           │   ├── I_REST_OIDCProvider.java
│                           │   ├── I_REST_OIDCService.java
│                           │   ├── I_REST_RefreshToken.java
│                           │   ├── I_REST_Resource.java
│                           │   ├── I_REST_Resource_Access.java
│                           │   ├── I_REST_Upload.java
│                           │   ├── I_REST_UploadChunk.java
│                           │   ├── I_REST_View.java
│                           │   ├── I_REST_ViewColumn.java
│                           │   ├── I_REST_ViewRelated.java
│                           │   ├── I_REST_Webhook_In.java
│                           │   ├── I_REST_Webhook_In_Log.java
│                           │   ├── I_REST_Webhook_Out.java
│                           │   ├── I_REST_Webhook_Out_Event.java
│                           │   ├── I_REST_Webhook_Out_Log.java
│                           │   ├── MAuthChallenge.java
│                           │   ├── MAuthToken.java
│                           │   ├── MOIDCProvider.java
│                           │   ├── MOIDCService.java
│                           │   ├── MRefreshToken.java
│                           │   ├── MRestResource.java
│                           │   ├── MRestResourceAccess.java
│                           │   ├── MRestUpload.java
│                           │   ├── MRestUploadChunk.java
│                           │   ├── MRestView.java
│                           │   ├── MRestViewColumn.java
│                           │   ├── MRestViewRelated.java
│                           │   ├── MRestWebhookIn.java
│                           │   ├── MRestWebhookInLog.java
│                           │   ├── MRestWebhookOut.java
│                           │   ├── MRestWebhookOutEvent.java
│                           │   ├── MRestWebhookOutLog.java
│                           │   ├── RESTEventHandler.java
│                           │   ├── X_REST_AuthToken.java
│                           │   ├── X_REST_OIDCProvider.java
│                           │   ├── X_REST_OIDCService.java
│                           │   ├── X_REST_RefreshToken.java
│                           │   ├── X_REST_Resource.java
│                           │   ├── X_REST_Resource_Access.java
│                           │   ├── X_REST_Upload.java
│                           │   ├── X_REST_UploadChunk.java
│                           │   ├── X_REST_View.java
│                           │   ├── X_REST_ViewColumn.java
│                           │   ├── X_REST_ViewRelated.java
│                           │   ├── X_REST_Webhook_In.java
│                           │   ├── X_REST_Webhook_In_Log.java
│                           │   ├── X_REST_Webhook_Out.java
│                           │   ├── X_REST_Webhook_Out_Event.java
│                           │   └── X_REST_Webhook_Out_Log.java
│                           ├── oidc
│                           │   ├── AbstractOIDCProvider.java
│                           │   ├── AuthenticatedUser.java
│                           │   ├── IOIDCProvider.java
│                           │   ├── amazoncognito
│                           │   │   └── AmazonCognitoProvider.java
│                           │   └── keycloak
│                           │       └── KeycloakProvider.java
│                           ├── process
│                           │   ├── ActiveInactiveToken.java
│                           │   ├── ExpireRefreshTokens.java
│                           │   ├── ExpireServerSecret.java
│                           │   └── WebhookRetryProcessor.java
│                           ├── util
│                           │   ├── ClusterUtil.java
│                           │   ├── ErrorBuilder.java
│                           │   ├── GridTabPaging.java
│                           │   ├── Paging.java
│                           │   └── ThreadLocalTrx.java
│                           ├── v1
│                           │   ├── ApplicationV1.java
│                           │   ├── auth
│                           │   │   ├── AuthService.java
│                           │   │   ├── LoginCredential.java
│                           │   │   ├── LoginParameters.java
│                           │   │   ├── LogoutParameters.java
│                           │   │   ├── RefreshParameters.java
│                           │   │   ├── filter
│                           │   │   │   ├── PresignedURL.java
│                           │   │   │   ├── RequestFilter.java
│                           │   │   │   ├── RequestSetLanguageFilter.java
│                           │   │   │   └── ResponseFilter.java
│                           │   │   └── impl
│                           │   │       └── AuthServiceImpl.java
│                           │   ├── jwt
│                           │   │   ├── DefaultTokenSecretProvider.java
│                           │   │   ├── ITokenSecretProvider.java
│                           │   │   ├── LoginClaims.java
│                           │   │   ├── SysConfigTokenSecretProvider.java
│                           │   │   └── TokenUtils.java
│                           │   └── resource
│                           │       ├── BatchRequestResource.java
│                           │       ├── CacheResource.java
│                           │       ├── ChartResource.java
│                           │       ├── FileResource.java
│                           │       ├── FormResource.java
│                           │       ├── HealthResource.java
│                           │       ├── InfoResource.java
│                           │       ├── MenuTreeResource.java
│                           │       ├── ModelResource.java
│                           │       ├── NodeResource.java
│                           │       ├── ProcessResource.java
│                           │       ├── ReferenceResource.java
│                           │       ├── ServerResource.java
│                           │       ├── StatusLineResource.java
│                           │       ├── TaskResource.java
│                           │       ├── UploadResource.java
│                           │       ├── ViewResource.java
│                           │       ├── WebhookInboundResource.java
│                           │       ├── WindowResource.java
│                           │       ├── WorkflowResource.java
│                           │       ├── file
│                           │       │   ├── FileAccess.java
│                           │       │   ├── FileInfo.java
│                           │       │   ├── FileStreamingOutput.java
│                           │       │   ├── GetFileInfoCallable.java
│                           │       │   ├── ReadFileCallable.java
│                           │       │   └── RemoteFileStreamingOutput.java
│                           │       ├── impl
│                           │       │   ├── BatchRequestResourseImpl.java
│                           │       │   ├── CacheResourceImpl.java
│                           │       │   ├── ChartResourceImpl.java
│                           │       │   ├── FileResourceImpl.java
│                           │       │   ├── FormResourceImpl.java
│                           │       │   ├── HealthResourceImpl.java
│                           │       │   ├── InfoResourceImpl.java
│                           │       │   ├── MenuTreeResourceImpl.java
│                           │       │   ├── ModelResourceImpl.java
│                           │       │   ├── NodeResourceImpl.java
│                           │       │   ├── ProcessResourceImpl.java
│                           │       │   ├── ReferenceResourceImpl.java
│                           │       │   ├── ServerResourceImpl.java
│                           │       │   ├── StatusLineResourceImpl.java
│                           │       │   ├── TaskResourceImpl.java
│                           │       │   ├── UploadResourceImpl.java
│                           │       │   ├── ViewResourceImpl.java
│                           │       │   ├── WebhookInboundResourceImpl.java
│                           │       │   ├── WindowResourceImpl.java
│                           │       │   ├── WorkflowResourceImpl.java
│                           │       │   └── YAMLSchema.java
│                           │       └── info
│                           │           ├── InfoWindow.java
│                           │           └── QueryResponse.java
│                           ├── v2
│                           │   └── auth
│                           │       ├── AuthRequestV2.java
│                           │       ├── AuthServiceV2.java
│                           │       ├── ContextRequestV2.java
│                           │       ├── MFARegistrationCompleteRequest.java
│                           │       ├── MFARegistrationRequest.java
│                           │       ├── MFAVerificationRequest.java
│                           │       └── impl
│                           │           └── AuthServiceV2Impl.java
│                           └── webhook
│                               ├── WebhookDispatcher.java
│                               ├── WebhookEventHandler.java
│                               ├── WebhookIPAllowlist.java
│                               ├── WebhookInboundHandler.java
│                               ├── WebhookPayloadTemplateResolver.java
│                               └── WebhookSignature.java
└── com.trekglobal.idempiere.rest.api.test
    └── src
        └── com
            └── trekglobal
                └── idempiere
                    └── rest
                        └── api
                            ├── json
                            │   ├── expand
                            │   │   └── test
                            │   │       ├── ExpandParserTest.java
                            │   │       └── ExpandUtilsTest.java
                            │   ├── filter
                            │   │   └── test
                            │   │       ├── ConvertedQueryTest.java
                            │   │       ├── DefaultQueryConverterTest.java
                            │   │       └── ODataUtilsTest.java
                            │   └── test
                            │       ├── Activator.java
                            │       ├── BinaryConverterTest.java
                            │       ├── DateTypeConverterTest.java
                            │       ├── DefaultPOSerializerTest.java
                            │       ├── ImageTypeConverterTest.java
                            │       ├── JSONTypeConverterTest.java
                            │       ├── LocationTypeConverterTest.java
                            │       ├── LookupTypeConverterTest.java
                            │       ├── ModelHelperTest.java
                            │       ├── NumericConverterTest.java
                            │       ├── ResponseUtilsTest.java
                            │       ├── RestTestCase.java
                            │       ├── RestUtilsTest.java
                            │       ├── TypeConverterUtilsTest.java
                            │       └── YesNoTypeConverterTest.java
                            ├── v1
                            │   ├── auth
                            │   │   └── impl
                            │   │       └── test
                            │   │           ├── AuthServiceImplTest.java
                            │   │           ├── AuthenticationTest.java
                            │   │           └── V1AuthPolicyTest.java
                            │   └── resource
                            │       └── impl
                            │           └── test
                            │               └── ProcessResourceImplTest.java
                            ├── v2
                            │   └── auth
                            │       └── impl
                            │           └── test
                            │               └── AuthServiceV2ImplTest.java
                            └── webhook
                                └── test
                                    └── WebhookIPAllowlistTest.java
```

### API v1 Resources

| Area | Function |
| --- | --- |
| Authentication | JWT login, context selection, refresh, logout and public keys. |
| Models | Business records, attachments and archive read operations. |
| Windows and forms | Window/tab and form operations. |
| Processes and files | Process execution, reports and generated file access. |
| References and information | References, lists, information windows, charts, menu trees and status lines. |
| Administration | Caches, nodes, servers, schedulers, tasks and health resources. |
| Workflow | Approval, rejection, forwarding, acknowledgement and user choices. |
| REST views | Configured view columns and related records. |
| Uploads | File uploads and upload chunks. |
| Webhooks | Inbound handling, outbound event delivery, signature/IP checks, logs and retries. |
| OIDC | Provider integration including Keycloak and Amazon Cognito. |

Exact operations and payloads are described in the bundled OpenAPI files.

### Progressive Authentication

The progressive sequence is credentials → client → TOTP when configured → role → organization → warehouse → final JWT. Direct login supplies the complete context with the credentials and completes MFA when required before token issuance.

| Endpoint under `/api/v2/auth` | Behavior |
| --- | --- |
| `POST /tokens` | Validates credentials. Returns allowed clients and a temporary challenge when context is incomplete; accepts complete context for direct login. |
| `PUT /tokens` | Selects the client or completes context validation. Returns `202 MFA_REQUIRED` when TOTP is required, otherwise advances context selection or issues final tokens. |
| `POST /mfa/verify` | Accepts `challengeToken`, `registrationId` and `code`. Consumes the MFA challenge; returns a rotated context challenge for progressive login or tokens for direct login. |
| `GET /roles` | Lists available roles after client/MFA validation. |
| `GET /organizations?role=...` | Lists organizations for the role. |
| `GET /warehouses?role=...&organization=...` | Lists warehouses for the selected context. |
| `GET /language` | Returns client language information. |

Context GET requests use the `X-Auth-Challenge` header. Successful context operations renew the challenge expiration. Challenges grant no access to business resources. Expired, consumed, blocked or wrong-stage challenges cannot be reused. Failed TOTP codes increment the attempt counter; the configured limit blocks the challenge.

A valid MFA registration without supported TOTP returns `403 unsupported_mfa_method`. Operational tokens are sent to protected resources as `Authorization: Bearer <token>`.

### TOTP Self-service

All these endpoints require an operational JWT.

| Endpoint under `/api/v2/auth` | Behavior |
| --- | --- |
| `GET /mfa/methods` | Lists active available TOTP methods. |
| `GET /mfa/registrations` | Lists registrations without secrets. |
| `POST /mfa/registrations` | Starts the authenticated user's registration; returns its ID, QR data URI, secret and expiration. |
| `POST /mfa/registrations/{id}/complete` | Confirms the owner's pending registration using a code, optional name and preference. |
| `DELETE /mfa/registrations/{id}` | Revokes a registration while retaining history. Administrators may revoke another user's registration only within the JWT client. |

The secret is exposed only in the initial registration response. REST enrollment supports TOTP; secret recovery and trusted-device enrollment are outside this flow.

### System Configuration

| Configuration | Default | Effect |
| --- | --- | --- |
| `REST_MFA_CHALLENGE_EXPIRATION_SECONDS` | `300` | MFA challenge lifetime in seconds. |
| `REST_CONTEXT_CHALLENGE_EXPIRATION_SECONDS` | `900` | Context challenge lifetime in seconds. |
| `REST_MFA_MAX_ATTEMPTS` | `5` | Failed TOTP attempt limit. |
| `REST_V1_AUTH_ENABLED` | `Y` | System-level switch for public v1 POST/PUT token endpoints. |

Applicable client settings override the first three defaults. Setting `REST_V1_AUTH_ENABLED=N` returns `410 v1_auth_disabled` from public v1 token creation/context endpoints while v2 token issuance remains available. Existing v1 business resources, refresh, logout and public keys retain their access rules.

### Models

| Models | Responsibility |
| --- | --- |
| `MAuthChallenge` | Internal `REST_AuthChallenge` table: SHA-256 token hash, user/context, flow state, expiration, attempts and consumption. Conditional updates control state transitions and consumption. Passwords, TOTP codes and raw challenge tokens are not stored here. |
| `MAuthToken`, `MRefreshToken` | Authentication tokens, refresh tokens and revocation. |
| `MOIDCProvider`, `MOIDCService` | OIDC provider and service configuration. |
| `MRestResource`, `MRestResourceAccess` | Resource definitions and access settings. |
| `MRestView`, `MRestViewColumn`, `MRestViewRelated` | Configurable REST views. |
| `MRestUpload`, `MRestUploadChunk` | Upload records and chunks. |
| Webhook models | Inbound/outbound settings, event subscriptions and delivery logs. |

The challenge table has no `AD_Table` registration, window or menu. MFA reuses existing core windows and records.

### Processes

| Class Name | Purpose | Main Parameters | Key Logic & Results |
| --- | --- | --- | --- |
| `ActiveInactiveToken` | Toggle a token. | Current authentication token record required. | Toggles active state and logs out the associated session. |
| `ExpireRefreshTokens` | Expire refresh tokens. | `REST_AllTokens` boolean; optional `AD_User_ID` filter. | Filters by client/user when applicable and reports the expired count. |
| `ExpireServerSecret` | Rotate signing secret. | No explicit process parameters. | Updates the server secret and marks authentication tokens inactive/expired. |
| `WebhookRetryProcessor` | Retry webhook deliveries. | Scheduler/process context. | Processes eligible pending deliveries and reports outcomes. |

### Events

| Target Model / Table | Event Timing / Topic | Functional Rule & Objective |
| --- | --- | --- |
| `AD_User` | `PO_AFTER_CHANGE` | Password change expires refresh tokens; deactivation also deactivates authentication tokens. |
| `AD_Role` | `PO_AFTER_CHANGE` | Deactivation deactivates associated authentication tokens. |
| `AD_Client` | `PO_AFTER_CHANGE` | Deactivation deactivates authentication tokens and expires refresh tokens. |
| Configured webhook tables | `PO_POST_CREATE`, `PO_POST_UPADTE`, `PO_POST_DELETE`; supported `DOC_AFTER_*` events | Creates deliveries for active subscriptions; document-event dispatch waits for commit. |

### Callouts

| Callout Class | Target Field / Column | Business Validation & UI Impact |
| --- | --- | --- |
| `CalloutRestOIDCService` | `REST_OIDCService.OIDC_AuthorityURL` | Substitutes the authority in the provider configuration URL template and sets the issuer URL. |

### Application Dictionary Metadata (2Pack)

| Package / File Name | Purpose & Dictionary Configurations |
| --- | --- |
| `2Pack_1.0.0.zip` | Incremental REST metadata; SQL schema changes. |
| `2Pack_1.0.1.zip` | Incremental REST metadata; `AD_EntityType`, `AD_Menu`, `AD_Package_Exp`. |
| `2Pack_1.0.10.zip` | Incremental REST metadata; `AD_Table`. |
| `2Pack_1.0.11.zip` | Incremental REST metadata; `AD_Column`, `AD_Field`. |
| `2Pack_1.0.12.zip` | Incremental REST metadata; `AD_Table`. |
| `2Pack_1.0.13.zip` | Upload large files #360; `AD_Column`, `AD_Element`, `AD_Reference`. |
| `2Pack_1.0.14.zip` | Incremental REST metadata; `AD_Message`, `AD_SysConfig`. |
| `2Pack_1.0.15.zip` | Display Logic of REST_ViewColumn.REST_ViewColumn; `AD_Field`. |
| `2Pack_1.0.16.zip` | Incremental REST metadata; `AD_IndexColumn`. |
| `2Pack_1.0.17.zip` | Incremental REST metadata; `AD_TableIndex`. |
| `2Pack_1.0.18.zip` | REST View window; `AD_Field`. |
| `2Pack_1.0.19.zip` | Incremental REST metadata; `AD_EntityType`, `AD_Menu`, `AD_Package_Exp`, `AD_Preference`, `AD_Process`, `AD_Scheduler`, `AD_SysConfig`, `AD_Window`. |
| `2Pack_1.0.2.zip` | Incremental REST metadata; `AD_EntityType`, `AD_Menu`. |
| `2Pack_1.0.20.zip` | Incremental REST metadata; `AD_Scheduler`, `AD_SysConfig`. |
| `2Pack_1.0.21.xml` | REST API v2 MFA authentication challenge storage; SQL schema changes. |
| `2Pack_1.0.21.zip` | REST API v2 MFA authentication challenge storage; SQL schema changes. |
| `2Pack_1.0.22.xml` | REST API v2 progressive MFA challenge state; SQL schema changes. |
| `2Pack_1.0.22.zip` | REST API v2 progressive MFA challenge state; SQL schema changes. |
| `2Pack_1.0.23.xml` | REST API v2 authentication SysConfig defaults; `AD_SysConfig`. |
| `2Pack_1.0.23.zip` | REST API v2 authentication SysConfig defaults; `AD_SysConfig`. |
| `2Pack_1.0.3.zip` | Incremental REST metadata; `AD_EntityType`, `REST_OIDCProvider`. |
| `2Pack_1.0.4.zip` | Incremental REST metadata; `AD_Table`. |
| `2Pack_1.0.5.zip` | Incremental REST metadata; `AD_Menu`. |
| `2Pack_1.0.6.zip` | Incremental REST metadata; `AD_Menu`. |
| `2Pack_1.0.7.zip` | Incremental REST metadata; `AD_Menu`, `AD_Message`. |
| `2Pack_1.0.8.zip` | Incremental REST metadata; `AD_Menu`, `AD_SysConfig`, `AD_Tab`, `AD_Table`. |
| `2Pack_1.0.9.zip` | Incremental REST metadata; `AD_Field`. |

Version 1.0.21 creates the challenge table and expiration index. Version 1.0.22 adds `ChallengeState`, `MFAVerifiedAt` and `MFA_Registration_ID`. Version 1.0.23 creates the four authentication configuration defaults. ZIP/XML variants of these versions represent the same migrations and must not be applied twice.

## Instructions

1. Deploy the OSGi bundle/P2 distribution in iDempiere 12 and ensure dependencies resolve.
2. Check successful migration entries in Pack In. Deployment alone does not establish that database changes completed. An installation at 1.0.21 requires 1.0.22 followed by 1.0.23 through the package import mechanism.
3. Verify the challenge columns and four settings in the existing System Configurator. Manage MFA through the existing core MFA windows.
4. Import [the progressive MFA collection](com.trekglobal.idempiere.rest.api/postman/idempiere-rest-v2-progressive-mfa.postman_collection.json). Set `baseUrl` to an HTTPS URL ending in `/api/v2`; set credentials and instance-specific context IDs.
5. Execute credentials, client selection, TOTP if requested, context queries and final JWT issuance. Replace the challenge whenever a response returns a new one. Post-response scripts store result variables; enter the current authenticator code manually.
6. Use the final JWT against the protected v1 business endpoints. Test both MFA and non-MFA users before disabling v1 authentication.
7. Avoid logging passwords, TOTP secrets/codes or complete tokens.

## Extra Links

- [API v1 OpenAPI](com.trekglobal.idempiere.rest.api/openapi/idempiere-rest.yml)
- [API v2 OpenAPI](com.trekglobal.idempiere.rest.api/openapi/idempiere-rest-v2.yml)
- [Upstream documentation](https://bxservice.github.io/idempiere-rest-docs/)
- [Upstream repository](https://github.com/bxservice/idempiere-rest)
