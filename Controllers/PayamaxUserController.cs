
using api.Model;
using api.UseCases.UploadPayamak;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using No1.Portal.Configs;

namespace api.Controllers;

[ApiController]
[Route("/payamax/all/")]
public class PayamaxUserController : ControllerBase{
    
    private readonly PayamaxContext payamaxContext;

    public PayamaxUserController(PayamaxContext payamaxContext)
    {
        this.payamaxContext = payamaxContext;
    }

    [HttpPost("upload")]
    public UploadPayamakOutput UploadPayamak(UploadPayamakInput input)
    {
        var count = payamaxContext.Payamaks.Count();
        throw new System.NotImplementedException();
    }
}