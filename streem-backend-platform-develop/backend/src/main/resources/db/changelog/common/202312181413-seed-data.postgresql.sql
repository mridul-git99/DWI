-- liquibase formatted sql

--changeset jagadeesh:202312181413-data-1
--comment : Adding email templates for Job Delay and Job OverDue


insert into email_templates(id, name, content) values(12, 'TEMPLATE_JOB_START_DUE', '
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:v="urn:schemas-microsoft-com:vml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <!--[if gte mso 9]><xml><o:OfficeDocumentSettings><o:AllowPNG/><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->

    <meta content="width=device-width" name="viewport">
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible">
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link href="https://fonts.googleapis.com/css?family=Nunito" rel="stylesheet">
    <!--<![endif]-->
    <style>body{margin:0;padding:0}table,td,tr{vertical-align:top;border-collapse:collapse}*{line-height:inherit}a[x-apple-data-detectors=true]{color:inherit!important;text-decoration:none!important}</style>
    <style id="media-query">@media (max-width:500px){.block-grid,.col{min-width:320px!important;max-width:100%!important;display:block!important}.block-grid{width:100%!important}.col{width:100%!important}.col_cont{margin:0 auto}img.fullwidth,img.fullwidthOnMobile{max-width:100%!important}.no-stack .col{min-width:0!important;display:table-cell!important}.no-stack.two-up .col{width:50%!important}.no-stack .col.num2{width:16.6%!important}.no-stack .col.num3{width:25%!important}.no-stack .col.num4{width:33%!important}.no-stack .col.num5{width:41.6%!important}.no-stack .col.num6{width:50%!important}.no-stack .col.num7{width:58.3%!important}.no-stack .col.num8{width:66.6%!important}.no-stack .col.num9{width:75%!important}.no-stack .col.num10{width:83.3%!important}.video-block{max-width:none!important}.mobile_hide{min-height:0;max-height:0;max-width:0;display:none;overflow:hidden;font-size:0}.desktop_hide{display:block!important;max-height:none!important}}</style>
</head>
<body class="clean-body" style="margin:0;padding:0;-webkit-text-size-adjust:100%;background-color:#f4f4f4">
<!--[if IE]><div class="ie-browser"><![endif]-->
<table bgcolor="#f4f4f4" cellpadding="0" cellspacing="0" class="nl-container" role="presentation" style="table-layout:fixed;vertical-align:top;min-width:320px;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;background-color:#f4f4f4;width:100%" valign="top" width="100%">
    <tbody>
    <tr style="vertical-align:top" valign="top">
        <td style="word-break:break-word;vertical-align:top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#f4f4f4">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#f4f4f4">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#f4f4f4"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#f4f4f4;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:50px; padding-bottom:0px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:50px;padding-bottom:0;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <div></div>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:32px; padding-bottom:32px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:32px;padding-bottom:32px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <div align="center" class="img-container center fixedwidth" style="padding-right:0;padding-left:0">
                                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 0px;padding-left: 0px;" align="center"><![endif]--><img align="center" alt="Alternate text" border="0" class="center fixedwidth" src="cid:leucine-blue-logo" style="text-decoration:none;-ms-interpolation-mode:bicubic;height:auto;border:0;width:100%;max-width:144px;display:block" title="Alternate text" width="144">
                                        <!--[if mso]></td></tr></table><![endif]-->
                                    </div>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 40px; padding-left: 40px; padding-top: 0px; padding-bottom: 32px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:0;padding-right:40px;padding-bottom:32px;padding-left:40px">
                                        <div style="line-height:1.2;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:14px">
                                            <p style="font-size:20px;line-height:1.2;word-break:break-word;text-align:center;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:24px;margin:0"><span style="font-size:20px">Reminder: Upcoming Scheduled Job</span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <table border="0" cellpadding="0" cellspacing="0" class="divider" role="presentation" style="table-layout:fixed;vertical-align:top;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;min-width:100%;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%" valign="top" width="100%">
                                        <tbody>
                                        <tr style="vertical-align:top" valign="top">
                                            <td class="divider_inner" style="word-break:break-word;vertical-align:top;min-width:100%;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%;padding-top:0;padding-right:10px;padding-bottom:0;padding-left:10px" valign="top">
                                                <table align="center" border="0" cellpadding="0" cellspacing="0" class="divider_content" role="presentation" style="table-layout:fixed;vertical-align:top;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;border-top:1px solid #eee;width:100%" valign="top" width="100%">
                                                    <tbody>
                                                    <tr style="vertical-align:top" valign="top">
                                                        <td style="word-break:break-word;vertical-align:top;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%" valign="top"><span></span></td>
                                                    </tr>
                                                    </tbody>
                                                </table>
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 32px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.5;padding-top:32px;padding-right:32px;padding-bottom:24px;padding-left:32px">
                                        <div style="line-height:1.5;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:18px">
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0">Hello,</p>
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0"> </p>
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0">This is a reminder that a scheduled job is set to start soon. Please ensure you are prepared to commence the job at the scheduled start time. Click the button below for more details.</p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <a href=${job} style="text-decoration:none;cursor:pointer">
                                        <div align="left" class="button-container" style="padding-top:0;padding-right:32px;padding-bottom:24px;padding-left:32px">
                                            <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;"><tr><td style="padding-top: 0px; padding-right: 32px; padding-bottom: 24px; padding-left: 32px" align="left"><v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word" href="" style="height:39pt; width:123.75pt; v-text-anchor:middle;" arcsize="8%" stroke="false" fillcolor="#1d84ff"><w:anchorlock/><v:textbox inset="0,0,0,0"><center style="color:#ffffff; font-family:Arial, sans-serif; font-size:14px"><![endif]-->
                                            <div style="text-decoration:none;display:inline-block;color:#fff;background-color:#1d84ff;border-radius:4px;-webkit-border-radius:4px;-moz-border-radius:4px;width:auto;width:auto;border-top:0 dotted #fff;border-right:0 dotted #fff;border-bottom:0 dotted #fff;border-left:0 dotted #fff;padding-top:10px;padding-bottom:10px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;text-align:center;mso-border-alt:none;word-break:keep-all"><span style="padding-left:20px;padding-right:20px;font-size:14px;display:inline-block"><span style="font-size:16px;line-height:2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:32px"><span data-mce-style="font-size: 14px; line-height: 28px;" style="font-size:14px;line-height:28px">View Job</span></span></span></div>
                                            <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->
                                        </div>
                                        <!--[if (!mso)&(!IE)]><!-->
                                    </a></div>

                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 00px; padding-bottom: 32px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.5;padding-top:00px;padding-right:32px;padding-bottom:32px;padding-left:32px">
                                        <div style="line-height:1.5;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:18px">
                                            <p style="font-size:12px;line-height:1.5;word-break:break-word;text-align:left;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:18px;margin:0"><span style="font-size:12px">Click the above button, or copy and paste the following link on your web browser: <span style="color:#1d84ff">${job}</span></span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 20px; padding-bottom: 60px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:20px;padding-right:32px;padding-bottom:60px;padding-left:32px">
                                        <div style="line-height:1.2;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:14px">
                                            <p style="font-size:14px;line-height:1.2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:17px;margin:0"><span style="font-size:14px">Warm Regards,</span></p>
                                            <p style="font-size:14px;line-height:1.2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:17px;margin:0"><span style="font-size:14px">Leucine</span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#f4f4f4">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#f4f4f4">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#f4f4f4"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#f4f4f4;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 20px; padding-bottom: 20px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:20px;padding-right:32px;padding-bottom:20px;padding-left:32px">
                                        <div style="line-height:1.2;font-size:12px;color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:14px">
                                            <p style="line-height:1.2;word-break:break-word;text-align:center;mso-line-height-alt:14px;margin:0"><span style="color:#000">To log in to Leucine, open this link in a web browser (e.g. Google Chrome) - <span style="color:#1d84ff">${streemlogin}</span></span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
        </td>
    </tr>
    </tbody>
</table>
<!--[if (IE)]></div><![endif]-->

</body></html>');



insert into email_templates(id, name, content) values(13, 'TEMPLATE_JOB_OVER_DUE', '
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:v="urn:schemas-microsoft-com:vml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <!--[if gte mso 9]><xml><o:OfficeDocumentSettings><o:AllowPNG/><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->

    <meta content="width=device-width" name="viewport">
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible">
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link href="https://fonts.googleapis.com/css?family=Nunito" rel="stylesheet">
    <!--<![endif]-->
    <style>body{margin:0;padding:0}table,td,tr{vertical-align:top;border-collapse:collapse}*{line-height:inherit}a[x-apple-data-detectors=true]{color:inherit!important;text-decoration:none!important}</style>
    <style id="media-query">@media (max-width:500px){.block-grid,.col{min-width:320px!important;max-width:100%!important;display:block!important}.block-grid{width:100%!important}.col{width:100%!important}.col_cont{margin:0 auto}img.fullwidth,img.fullwidthOnMobile{max-width:100%!important}.no-stack .col{min-width:0!important;display:table-cell!important}.no-stack.two-up .col{width:50%!important}.no-stack .col.num2{width:16.6%!important}.no-stack .col.num3{width:25%!important}.no-stack .col.num4{width:33%!important}.no-stack .col.num5{width:41.6%!important}.no-stack .col.num6{width:50%!important}.no-stack .col.num7{width:58.3%!important}.no-stack .col.num8{width:66.6%!important}.no-stack .col.num9{width:75%!important}.no-stack .col.num10{width:83.3%!important}.video-block{max-width:none!important}.mobile_hide{min-height:0;max-height:0;max-width:0;display:none;overflow:hidden;font-size:0}.desktop_hide{display:block!important;max-height:none!important}}</style>
</head>
<body class="clean-body" style="margin:0;padding:0;-webkit-text-size-adjust:100%;background-color:#f4f4f4">
<!--[if IE]><div class="ie-browser"><![endif]-->
<table bgcolor="#f4f4f4" cellpadding="0" cellspacing="0" class="nl-container" role="presentation" style="table-layout:fixed;vertical-align:top;min-width:320px;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;background-color:#f4f4f4;width:100%" valign="top" width="100%">
    <tbody>
    <tr style="vertical-align:top" valign="top">
        <td style="word-break:break-word;vertical-align:top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#f4f4f4">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#f4f4f4">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#f4f4f4"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#f4f4f4;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:50px; padding-bottom:0px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:50px;padding-bottom:0;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <div></div>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:32px; padding-bottom:32px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:32px;padding-bottom:32px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <div align="center" class="img-container center fixedwidth" style="padding-right:0;padding-left:0">
                                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 0px;padding-left: 0px;" align="center"><![endif]--><img align="center" alt="Alternate text" border="0" class="center fixedwidth" src="cid:leucine-blue-logo" style="text-decoration:none;-ms-interpolation-mode:bicubic;height:auto;border:0;width:100%;max-width:144px;display:block" title="Alternate text" width="144">
                                        <!--[if mso]></td></tr></table><![endif]-->
                                    </div>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 40px; padding-left: 40px; padding-top: 0px; padding-bottom: 32px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:0;padding-right:40px;padding-bottom:32px;padding-left:40px">
                                        <div style="line-height:1.2;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:14px">
                                            <p style="font-size:20px;line-height:1.2;word-break:break-word;text-align:center;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:24px;margin:0"><span style="font-size:20px">Action Required: Overdue Scheduled Job</span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <table border="0" cellpadding="0" cellspacing="0" class="divider" role="presentation" style="table-layout:fixed;vertical-align:top;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;min-width:100%;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%" valign="top" width="100%">
                                        <tbody>
                                        <tr style="vertical-align:top" valign="top">
                                            <td class="divider_inner" style="word-break:break-word;vertical-align:top;min-width:100%;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%;padding-top:0;padding-right:10px;padding-bottom:0;padding-left:10px" valign="top">
                                                <table align="center" border="0" cellpadding="0" cellspacing="0" class="divider_content" role="presentation" style="table-layout:fixed;vertical-align:top;border-spacing:0;border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;border-top:1px solid #eee;width:100%" valign="top" width="100%">
                                                    <tbody>
                                                    <tr style="vertical-align:top" valign="top">
                                                        <td style="word-break:break-word;vertical-align:top;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%" valign="top"><span></span></td>
                                                    </tr>
                                                    </tbody>
                                                </table>
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 32px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.5;padding-top:32px;padding-right:32px;padding-bottom:24px;padding-left:32px">
                                        <div style="line-height:1.5;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:18px">
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0">Hello,</p>
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0"> </p>
                                            <p style="font-size:14px;line-height:1.5;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:21px;margin:0">This is an urgent notification that a scheduled job is now overdue. Immediate action is required to address the overdue job. Click the button below for more details.</p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <a href=${job} style="text-decoration:none;cursor:pointer">
                                        <div align="left" class="button-container" style="padding-top:0;padding-right:32px;padding-bottom:24px;padding-left:32px">
                                            <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;"><tr><td style="padding-top: 0px; padding-right: 32px; padding-bottom: 24px; padding-left: 32px" align="left"><v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word" href="" style="height:39pt; width:123.75pt; v-text-anchor:middle;" arcsize="8%" stroke="false" fillcolor="#1d84ff"><w:anchorlock/><v:textbox inset="0,0,0,0"><center style="color:#ffffff; font-family:Arial, sans-serif; font-size:14px"><![endif]-->
                                            <div style="text-decoration:none;display:inline-block;color:#fff;background-color:#1d84ff;border-radius:4px;-webkit-border-radius:4px;-moz-border-radius:4px;width:auto;width:auto;border-top:0 dotted #fff;border-right:0 dotted #fff;border-bottom:0 dotted #fff;border-left:0 dotted #fff;padding-top:10px;padding-bottom:10px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;text-align:center;mso-border-alt:none;word-break:keep-all"><span style="padding-left:20px;padding-right:20px;font-size:14px;display:inline-block"><span style="font-size:16px;line-height:2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:32px"><span data-mce-style="font-size: 14px; line-height: 28px;" style="font-size:14px;line-height:28px">View Job</span></span></span></div>
                                            <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->
                                        </div>
                                        <!--[if (!mso)&(!IE)]><!-->
                                    </a></div>

                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 00px; padding-bottom: 32px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.5;padding-top:00px;padding-right:32px;padding-bottom:32px;padding-left:32px">
                                        <div style="line-height:1.5;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:18px">
                                            <p style="font-size:12px;line-height:1.5;word-break:break-word;text-align:left;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:18px;margin:0"><span style="font-size:12px">Click the above button, or copy and paste the following link on your web browser: <span style="color:#1d84ff">${job}</span></span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:transparent">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#fff">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#fff">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#ffffff"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#ffffff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 20px; padding-bottom: 60px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:20px;padding-right:32px;padding-bottom:60px;padding-left:32px">
                                        <div style="line-height:1.2;font-size:12px;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;color:#000;mso-line-height-alt:14px">
                                            <p style="font-size:14px;line-height:1.2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:17px;margin:0"><span style="font-size:14px">Warm Regards,</span></p>
                                            <p style="font-size:14px;line-height:1.2;word-break:break-word;font-family:Nunito,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:17px;margin:0"><span style="font-size:14px">Leucine</span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <div style="background-color:#f4f4f4">
                <div class="block-grid" style="min-width:320px;max-width:480px;overflow-wrap:break-word;word-wrap:break-word;word-break:break-word;Margin:0 auto;background-color:#f4f4f4">
                    <div style="border-collapse:collapse;display:table;width:100%;background-color:#f4f4f4">
                        <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#f4f4f4"><![endif]-->
                        <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#f4f4f4;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                        <div class="col num12" style="min-width:320px;max-width:480px;display:table-cell;vertical-align:top;width:480px">
                            <div class="col_cont" style="width:100%!important">
                                <!--[if (!mso)&(!IE)]><!-->
                                <div style="border-top:0 solid transparent;border-left:0 solid transparent;border-bottom:0 solid transparent;border-right:0 solid transparent;padding-top:5px;padding-bottom:5px;padding-right:0;padding-left:0">
                                    <!--<![endif]-->
                                    <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 32px; padding-left: 32px; padding-top: 20px; padding-bottom: 20px; font-family: Arial, sans-serif"><![endif]-->
                                    <div style="color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;line-height:1.2;padding-top:20px;padding-right:32px;padding-bottom:20px;padding-left:32px">
                                        <div style="line-height:1.2;font-size:12px;color:#000;font-family:&#39;Nunito&#39;,Arial,&#39;Helvetica Neue&#39;,Helvetica,sans-serif;mso-line-height-alt:14px">
                                            <p style="line-height:1.2;word-break:break-word;text-align:center;mso-line-height-alt:14px;margin:0"><span style="color:#000">To log in to Leucine, open this link in a web browser (e.g. Google Chrome) - <span style="color:#1d84ff">${streemlogin}</span></span></p>
                                        </div>
                                    </div>
                                    <!--[if mso]></td></tr></table><![endif]-->
                                    <!--[if (!mso)&(!IE)]><!-->
                                </div>
                                <!--<![endif]-->
                            </div>
                        </div>
                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->
                    </div>
                </div>
            </div>
            <!--[if (mso)|(IE)]></td></tr></table><![endif]-->
        </td>
    </tr>
    </tbody>
</table>
<!--[if (IE)]></div><![endif]-->

</body></html>');
