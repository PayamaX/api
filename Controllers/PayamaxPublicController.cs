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
[Route("/payamax/public/")]
[ApiExplorerSettings(GroupName = "public")]
public class PayamaxPublicController(IPayamaksContract payamaksContract, ILogger<PayamaxPublicController> logger) : ControllerBase
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="input"></param>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    [HttpPost("upload")]
    public Task<UploadPayamakOutput> UploadPayamak(UploadPayamakInput input, CancellationToken cancellationToken = default)
    {
        logger.LogInformation("UploadPayamak called");
        return payamaksContract.Upload(input, cancellationToken);
    }
}