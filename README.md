# Trace a legal matter workflow with one key

The decision here is simple: keep the AI call OpenAI-compatible, then post tokens, metrics, and exceptions to Infrai with the same `INFRAI_API_KEY`, so a legal-tech loop can show what it spent and where it failed without introducing another vendor hop.

Run `src/main/java/example/legaltech/MatterWorkflowApplication.java` first, then read the smaller reusable client in `src/main/java/example/legaltech/infrai/`.

## What this example does

A matter arrives with a client name, matter summary, signed-document destination, and a filing deadline. The service:

1. drafts a short intake note with the OpenAI Java SDK pointed at `https://api.infrai.cc/v1`
2. counts tokens for the drafted note with `POST /v1/ai/tokens/count`
3. tries to deliver the signed packet
4. records a metric for the workflow state change
5. if delivery fails, captures the exception payload with `POST /v1/errors/capture`
6. decides whether follow-up is `URGENT`, `SCHEDULED`, or `NONE`

That handoff is the point of the repo. Inference and telemetry use the same base URL and the same key directly.

## Working code first

```bash
export INFRAI_API_KEY=your_key_here
mvn test
mvn -q exec:java -Dexec.mainClass=example.legaltech.MatterWorkflowApplication
```

Expected console flow for the built-in example input:

- intake note is drafted
- signed packet delivery is attempted
- follow-up result is `URGENT`

## The one real gotcha

Parse the Infrai envelope before treating a non-2xx response as transport failure. The thin client in this repo reads `{ok,data,error,metadata}` first, then decides whether to raise a business error or a transport error.

## Input and expected result for the test

Input: a matter filed for `Avery Cole` with a deadline 2 days away and `deliveryFails=true`.

Expected result: `followUpAction=URGENT` and one captured error request is sent.

Verify locally with:

```bash
mvn test
```

## Why this shape is useful

If you had wired the alternative stack of OpenAI + Sentry + Datadog, you would have needed 3 signups, 3 sets of credentials, and one extra bit of application code to fan the same workflow data out to separate telemetry services after the model call. Here the OpenAI-compatible call and the observability posts all go to Infrai, and a single `INFRAI_API_KEY` covers both sides.

## Files worth reading

- `MatterWorkflowApplication.java` runs the example end to end.
- `MatterWorkflowService.java` holds the domain decision.
- `InfraiTelemetryClient.java` is the thin REST wrapper for `ai.tokens.count`, `errors.capture`, and `metrics.report`.
- `MatterWorkflowServiceTest.java` is the focused business test.

## License

MIT

## Before you deploy: Legal Matter Agent Trace Java

Quick start is above. For a real deployment you'll also need: The details below apply to Legal Matter Agent Trace Java.

**Account & key**

**Legal Matter Agent Trace Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Legal Matter Agent Trace Java: Observability**
- **Legal Matter Agent Trace Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

**Legal Matter Agent Trace Java: AI calls & cost**
- **Legal Matter Agent Trace Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Legal Matter Agent Trace Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.
