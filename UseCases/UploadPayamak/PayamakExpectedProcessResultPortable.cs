using PayamaX.Portal.Commons;

namespace PayamaX.Portal.UseCases.UploadPayamak;

/// <summary>
/// 
/// </summary>
/// <param name="Payamak"></param>
/// <param name="DetectedUsabilityClass"></param>
/// <param name="ExpectedUsabilityClass"></param>
/// <param name="PayamakRuleProcessResults"></param>
public record PayamakExpectedProcessResultPortable(
    Payamak Payamak,
    PayamakUsabilityClass DetectedUsabilityClass,
    PayamakUsabilityClass ExpectedUsabilityClass,
    IReadOnlyCollection<PayamakRuleProcessResult> PayamakRuleProcessResults
    );

/// <summary>
/// 
/// </summary>
/// <param name="Origin"></param>
/// <param name="BodyText"></param>
/// <param name="BodyHash"></param>
/// <param name="ReceivedEpochMillis"></param>
public record Payamak(PayamakOriginComponent Origin, string? BodyText, string BodyHash, long ReceivedEpochMillis);

/// <summary>
/// 
/// </summary>
/// <param name="Id"></param>
/// <param name="Rating"></param>
/// <param name="Comment"></param>
/// <param name="Details"></param>
public record PayamakRuleProcessResult(string Id, double? Rating, string Comment, object Details);

/// <summary>
/// 
/// </summary>
public enum PayamakUsabilityClass
{
    /// <summary>
    /// 
    /// </summary>
    Usable,
    
    /// <summary>
    /// 
    /// </summary>
    Spam
}