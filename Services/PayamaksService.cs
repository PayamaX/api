using PayamaX.Portal.Contracts;
using PayamaX.Portal.Model;
using PayamaX.Portal.UseCases.UploadPayamak;
using PayamaX.Portal.Utility;

namespace PayamaX.Portal.Services;

public class PayamaksService(PayamaxRepo repo) : IPayamaksContract
{
    private readonly PayamaxRepo Repo = repo;

    public async Task<UploadPayamakOutput> Upload(UploadPayamakInput input, CancellationToken cancellationToken = default)
    {
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
        await Repo.Persist(entities, cancellationToken);
        return new UploadPayamakOutput();
    }

    public Task<IList<ExpectedPayamakProcessResult>> List(CancellationToken cancellationToken)
    {
        return Repo.List(cancellationToken);
    }

    private string CalculateHash(string? text)
    {
        return "Uncalced";
    }
}