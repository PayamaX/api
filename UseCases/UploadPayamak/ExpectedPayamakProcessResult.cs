using PayamaX.Portal.Commons;

namespace PayamaX.Portal.UseCases.UploadPayamak;

public record ExpectedPayamakProcessResult(
    Payamak Payamak,
    PayamakUsabilityClass DetectedUsabilityClass,
    PayamakUsabilityClass ExpectedUsabilityClass,
    List<PayamakRuleProcessResult> PayamakRuleProcessResults
    );

public record Payamak(PayamakOriginComponent Origin, string? BodyText, string BodyHash, long ReceivedEpochMillis);

public record PayamakRuleProcessResult(string Id, double? Rating, string Comment, object Details);

public enum PayamakUsabilityClass
{
    Usable,
    Spam
}