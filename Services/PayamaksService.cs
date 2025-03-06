using PayamaX.Portal.Contracts;
using PayamaX.Portal.Model;
using PayamaX.Portal.UseCases.UploadPayamak;
using PayamaX.Portal.Utility;

namespace PayamaX.Portal.Services;

/// <summary>
/// 
/// </summary>
/// <param name="repo"></param>
public class PayamaksService(PayamaxRepo repo) : IPayamaksContract
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="input"></param>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public async Task<UploadPayamakOutput> Upload(UploadPayamakInput input, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(input);
        var entities = input.PayamakProcessResults.Select(payamakInput => new PayamakExpectedProcessResultEntity()
            {
                BodyHash = payamakInput.Payamak.BodyText.IsUsable()
                    ? CalculateHash(payamakInput.Payamak.BodyText)
                    : payamakInput.Payamak.BodyHash,
                BodyText = payamakInput.Payamak.BodyText,
                Origin = payamakInput.Payamak.Origin,
                DetectedUsabilityClass = payamakInput.DetectedUsabilityClass,
                ExpectedUsabilityClass = payamakInput.ExpectedUsabilityClass,
                ReceivedEpochMillis = payamakInput.Payamak.ReceivedEpochMillis,
            })
            .ToList();
        await repo.Persist(entities, cancellationToken).ConfigureAwait(false);
        return new UploadPayamakOutput();
    }

    /// <summary>
    /// 
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public async Task<IList<PayamakExpectedProcessResultPortable>> List(CancellationToken cancellationToken = default)
    {
        var list = await repo.List(cancellationToken).ConfigureAwait(false);
        var portables = list.Select(x => x.Portable());
        return portables.ToList();
    }

    private static string CalculateHash(string? text)
    {
        return "Uncalced";
    }
}