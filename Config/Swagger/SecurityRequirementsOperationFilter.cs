using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace PayamaX.Portal.Config.Swagger;

/// <summary>
/// 
/// </summary>
public class SecurityRequirementsOperationFilter : IOperationFilter
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="operation"></param>
    /// <param name="context"></param>
    public void Apply(OpenApiOperation operation, OperationFilterContext context)
    {
        // Policy names map to scopes
        var requiredScopes = context.MethodInfo
            .GetCustomAttributes(true)
            .OfType<AuthorizeAttribute>()
            .Select(attr => attr.Policy)
            .Distinct()
            .ToList();

        if (requiredScopes.Count == 0) return;
        operation.Responses.Add("401", new OpenApiResponse { Description = "Unauthorized" });
        operation.Responses.Add("403", new OpenApiResponse { Description = "Forbidden" });

        var bearerScheme = new OpenApiSecurityScheme
        {
            Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "bearer" }
        };

        operation.Security = new List<OpenApiSecurityRequirement>
        {
            new()
            {
                [ bearerScheme ] = requiredScopes.ToList()
            }
        };
    }
}