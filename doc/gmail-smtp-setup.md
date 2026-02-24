# Gmail SMTP Configuration Guide

How to configure Gmail as the SMTP provider for sending verification emails, password reset links, and admin invitations.

## Prerequisites

| Requirement | Details |
|-------------|---------|
| Gmail account | Any `@gmail.com` or Google Workspace account |
| 2-Step Verification | Must be enabled on the Gmail account |
| Application configured | `.env` file exists (run `bin/setup.sh` first) |

## Step 1 -- Enable 2-Step Verification

If 2-Step Verification is already enabled on your Google account, skip to Step 2.

1. Go to <https://myaccount.google.com/security>
2. Under **How you sign in to Google**, click **2-Step Verification**
3. Follow Google's prompts to enable it (you will need a phone number or authenticator app)
4. Once enabled, you will see a confirmation on the security page

## Step 2 -- Generate an App Password

Google App Passwords are 16-character credentials that allow applications to authenticate without your main account password.

1. Go to <https://myaccount.google.com/apppasswords>
   - This page is only accessible if 2-Step Verification is enabled
2. Enter an app name (e.g., `UserMgmt` or `Spring Boot App`)
3. Click **Create** (or **Generate**)
4. Google displays a 16-character password in groups of four (e.g., `abcd efgh ijkl mnop`)
5. **Copy the password immediately** -- it is shown only once

Spaces in the password are optional. Both `abcdefghijklmnop` and `abcd efgh ijkl mnop` work.

## Step 3 -- Configure .env

Open your `.env` file (or `.env.local` for personal overrides) and set the mail variables:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=abcd-efgh-ijkl-mnop
MAIL_FROM=your-gmail@gmail.com
```

Variable details:

| Variable | Value |
|----------|-------|
| `MAIL_HOST` | Always `smtp.gmail.com` for Gmail |
| `MAIL_PORT` | Always `587` (STARTTLS) |
| `MAIL_USERNAME` | Your full Gmail address |
| `MAIL_PASSWORD` | The 16-character App Password from Step 2 (**not** your Gmail account password) |
| `MAIL_FROM` | Should match `MAIL_USERNAME` -- Gmail overrides the From address to match the authenticated account |

## Step 4 -- Verify Email Delivery

1. Start the application:
   ```bash
   # Dev mode (H2 database):
   ./bin/run-spring-dev-mode.sh

   # Or prod mode (Docker + PostgreSQL):
   ./bin/run-prod.sh
   ```
2. Register a new user at <http://localhost:8080/register>
3. Check the registered email inbox for the verification email
4. If the email arrives, SMTP is configured correctly

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `AuthenticationFailedException` | App Password is incorrect, or 2-Step Verification is not enabled | Regenerate the App Password at <https://myaccount.google.com/apppasswords> and update `MAIL_PASSWORD` |
| `MailConnectException` / connection timeout | Firewall or network blocking port 587 | Check that outbound TCP port 587 is allowed on your network |
| `SMTPSendFailedException: 550` | Gmail daily sending limit reached | Gmail allows ~500 emails/day for personal accounts and ~2000/day for Google Workspace |
| "Less Secure App Access" references | Deprecated by Google in May 2022 | App Passwords are the only supported method -- ignore any legacy references to Less Secure App Access |
| From address mismatch | Gmail overrides the From header to match the authenticated account | Set `MAIL_FROM` to match `MAIL_USERNAME`, or configure a Gmail alias in your Google account settings |

## Alternative SMTP Providers

This application works with any SMTP provider. To use a different provider (Mailgun, SendGrid, Amazon SES, etc.), change `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD` in `.env` to match the provider's SMTP settings. See `doc/04-deployment.md` for the full production configuration checklist.

## References

- [Google App Passwords support page](https://support.google.com/accounts/answer/185833)
- [`.env.example`](../.env.example) -- quick reference for all environment variables
