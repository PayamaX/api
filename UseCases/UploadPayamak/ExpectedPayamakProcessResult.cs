using api.Commons;

namespace api.UseCases.UploadPayamak;

public record ExpectedPayamakProcessResult(
    Payamak Payamak,
    PayamakUsabilityClass DetectedUsabilityClass,
    PayamakUsabilityClass ExpectedUsabilityClass,
    List<PayamakRuleProcessResult> PayamakRuleProcessResults
    );

public record Payamak(PayamakOrigin Origin, string? BodyText, string BodyHash, long ReceivedEpochMillis);

public record PayamakRuleProcessResult(string Id, double? Rating, string Comment);

public enum PayamakUsabilityClass
{
    Usable,
    Spam
}