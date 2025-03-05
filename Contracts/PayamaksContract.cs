using PayamaX.Portal.Model;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Contracts;

/// <summary>
/// 
/// </summary>
public interface IPayamaksContract
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="input"></param>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    Task<UploadPayamakOutput> Upload(UploadPayamakInput input, CancellationToken cancellationToken = default);

    /// <summary>
    /// 
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    Task<IList<PayamakExpectedProcessResultPortable>> List(CancellationToken cancellationToken = default);
}