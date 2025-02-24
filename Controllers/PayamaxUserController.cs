
using api.UseCases.UploadPayamak;
using Microsoft.AspNetCore.Mvc;
using No1.Portal.Configs;

namespace api.Controllers;

[ApiController]
[Route("/payamax/all/")]
public class PayamaxUserController : ControllerBase{
    
    private readonly ConnectionString connectionString;

    public PayamaxUserController(ConnectionString connectionString)
    {
        this.connectionString = connectionString;
    }

    [HttpPost("upload")]
    public UploadPayamakOutput UploadPayamak(UploadPayamakInput input)
    {
        throw new System.NotImplementedException();
    }
}