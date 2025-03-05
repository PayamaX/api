using Microsoft.AspNetCore.Mvc.Controllers;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace PayamaX.Portal.Config;

/// <summary>
/// 
/// </summary>
public class IdentityDocumentFilter : IDocumentFilter
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="swaggerDoc"></param>
    /// <param name="context"></param>
    public void Apply(OpenApiDocument swaggerDoc, DocumentFilterContext context)
    {
        ArgumentNullException.ThrowIfNull(swaggerDoc);
        ArgumentNullException.ThrowIfNull(context);
        foreach (var apiDescription in context.ApiDescriptions)
        {
            if (apiDescription.RelativePath?.StartsWith("identity/", StringComparison.InvariantCultureIgnoreCase) ?? false)
            {
                apiDescription.GroupName = "identity";
            }
        }
    }
}