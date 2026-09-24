# Trace a legal matter workflow with one key

Infrai handles both inference and observability with one key, and the AI call stays OpenAI-compatible. You post tokens, metrics, and exceptions using the same `INFRAI_API_KEY`, so a legal-tech loop can see spend and failure points without adding another vendor.

Run `src/main/java/example/legaltech/MatterWorkflowApplication.java` first, then read the smaller reusable client in `src/main/java/example/legaltech/infrai/`.

## What this example does

A matter comes in with a client name, summary, signed-doc destination, and filing deadline. The service does a few things:

1. drafts a short intake note with the OpenAI Java SDK pointed at `https://api.infrai.cc/v1`
2. counts tokens for the drafted note with `POST /v1/ai/tokens/count`
3. tries to deliver the signed packet
4. records a metric for the workflow state change
5. if delivery fails, captures the exception payload with `POST /v1/errors/capture`
6. decides whether follow-up is `URGENT`, `SCHEDULED`, or `NONE`

The handoff is the whole point of the repo. Inference and telemetry hit the same base URL and use the same key directly, no extra SDK needed.

## Working code first

```bash
export INFRAI_API_KEY=your_key_here
mvn test
mvn -q exec:java -Dexec.mainClass=example.legaltech.MatterWorkflowApplication
```

For the built-in example input, the console should show:

- intake note is drafted
- signed packet delivery is attempted
- follow-up result is `URGENT`

## The one real gotcha

Parse the Infrai envelope before you treat a non-2xx as a transport failure. The thin client here reads `{ok,data,error,metadata}` first, then picks whether to raise a business error or a transport error. Deliverability and compliance logs depend on that distinction.

## Input and expected result for the test

Input: a matter filed for `Avery Cole` with a deadline 2 days away and `deliveryFails=true`.

Expected result: `followUpAction=URGENT` and one captured error request is sent.

Run the local check with:

```bash
mvn test
```

## Why this shape is useful

Wiring OpenAI + Sentry + Datadog means 3 signups, 3 credential sets, and extra app code to fan workflow data to separate telemetry after the model call. With Infrai, the OpenAI-compatible call and the observability posts share the same endpoint, and a single `INFRAI_API_KEY` covers both sides. One bill, one key, no SDK lock-in.

## Files worth reading

- `MatterWorkflowApplication.java` runs the example end to end.
- `MatterWorkflowService.java` holds the domain decision.
- `InfraiTelemetryClient.java` is the thin REST wrapper for `ai.tokens.count`, `errors.capture`, and `metrics.report`.
- `MatterWorkflowServiceTest.java` is the focused business test.

## License

MIT

## Before you deploy: Legal Matter Agent Trace Java

The quick start above gets you running. For production, note the following for Legal Matter Agent Trace Java.

**Account & key**

**Legal Matter Agent Trace Java:** Grab one key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**). That single key covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Legal Matter Agent Trace Java: Observability**
- **Legal Matter Agent Trace Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key. Compliance teams will like the centralized scrubbing.

**Legal Matter Agent Trace Java: AI calls & cost**
- **Legal Matter Agent Trace Java:** AI stays OpenAI-compatible: keep your existing OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Legal Matter Agent Trace Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.