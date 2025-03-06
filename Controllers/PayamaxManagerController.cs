using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Controllers;

/// <summary>
/// 
/// </summary>
/// <param name="payamaksContract"></param>
/// <param name="logger"></param>
[ApiController]
[Route("/payamax/manager/")]
[ApiExplorerSettings(GroupName = "manager")]
[Authorize("bearer")]
public class PayamaxManagerController(IPayamaksContract payamaksContract, ILogger<PayamaxManagerController> logger) : ControllerBase
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    [HttpGet("download")]
    public Task<IList<PayamakExpectedProcessResultPortable>> DownloadPayamaks(CancellationToken cancellationToken = default)
    {
        logger.LogDebug("DownloadPayamaks called");
        return payamaksContract.List(cancellationToken);
    }
}