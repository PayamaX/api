using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Contracts;

public interface IPayamaksContract
{
    Task<UploadPayamakOutput> Upload(UploadPayamakInput input, CancellationToken cancellationToken = default);
    Task<IList<ExpectedPayamakProcessResult>> List(CancellationToken cancellationToken);
}