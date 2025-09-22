-- liquibase formatted sql

--changeset mokshesh:202404140229-seed-data-1
--comment: merge jaas email templates into dwi email templates

INSERT INTO public.email_templates (id, "name", "content") VALUES(14, 'REGISTER', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >You are invited to start using Leucine</span
                              ><br /><span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >for digitalising Cleaning</span
                              >
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333"
                                >Hello ${userFullName},</span
                              ><br /><br /><span style="color: #333333"
                                >Your company is now using Leucine to digitalise
                                cleaning work instructions at your Facility. You
                                are invited to register.</span
                              ><br /><br /><span style="color: #333333"
                                >Click on "Register with Leucine" button below and
                                use Secret Key to identify your self for the
                                registration process.</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #ffffff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    background-color: #e7f1fd;
                    margin: 0 24px;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#e7f1fd"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#e7f1fd;width:480px; border-top: 0px solid #000000; border-left: 0px solid #000000; border-bottom: 0px solid #000000; border-right: 0px solid #000000;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid #000000;
                          border-left: 0px solid #000000;
                          border-bottom: 0px solid #000000;
                          border-right: 0px solid #000000;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 12px; padding-bottom: 12px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.5;
                            padding-top: 8px;
                            padding-bottom: 8px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.5;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 21px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                word-break: break-word;
                                text-align: center;
                                mso-line-height-alt: 30px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 24px;
                                  color: #333333;
                                  font-weight: bold;
                                  line-height: 1.25;
                                "
                                >Secret Key : ${token}</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="left"
                          class="button-container"
                          style="padding: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;"><tr><td style="padding-top: 24px; padding-right: 24px; padding-bottom: 12px; padding-left: 24px" align="left"><v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word" href="" style="height:31.5pt;width:171.75pt;v-text-anchor:middle;" arcsize="10%" stroke="false" fillcolor="#1d84ff"><w:anchorlock/><v:textbox inset="0,0,0,0"><center style="color:#ffffff; font-family:Arial, sans-serif; font-size:16px"><![endif]-->
                          <a
                            href="${registrationLink}"
                            target="_blank"
                          >
                            <div
                              style="
                                text-decoration: none;
                                display: inline-block;
                                color: #ffffff;
                                background-color: #1d84ff;
                                border-radius: 4px;
                                cursor: pointer;
                                -webkit-border-radius: 4px;
                                -moz-border-radius: 4px;
                                width: auto;
                                width: auto;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                text-align: center;
                                mso-border-alt: none;
                                word-break: keep-all;
                              "
                            >
                              <span
                                style="
                                  padding: 12px 16px;
                                  font-size: 14px;
                                  line-height: 1.14;
                                  letter-spacing: 0.16px;
                                  display: inline-block;
                                  letter-spacing: undefined;
                                "
                                >Register with Leucine</span
                              >
                            </div>
                          </a>
                          <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                color: #666666;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              You may copy paste this link into your web browser
                              -
                              <a
                                href="${registrationLink}"
                                target="_blank"
                                style="text-decoration: none"
                              >
                                <span style="color: #1d84ff"
                                  >${registrationLink}</span
                                >
                              </a>
                            </p>
                            <!-- <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >

                            </p> -->
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(15, 'REMINDER_TO_REGISTER', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >A reminder to complete your
                                </span
                              ><br /><span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >registration with Leucine</span
                              >
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333"
                                >Hello ${userFullName},</span
                              ><br /><br /><span style="color: #333333"
                                >This is to remind you that your registration for Leucine is still pending.</span
                              ><br /><br /><span style="color: #333333"
                                >Please click on "Register with Leucine" button below and
                                use Secret Key to identify your self for the
                                registration process.</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #ffffff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    background-color: #e7f1fd;
                    margin: 0 24px;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#e7f1fd"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#e7f1fd;width:480px; border-top: 0px solid #000000; border-left: 0px solid #000000; border-bottom: 0px solid #000000; border-right: 0px solid #000000;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid #000000;
                          border-left: 0px solid #000000;
                          border-bottom: 0px solid #000000;
                          border-right: 0px solid #000000;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 12px; padding-bottom: 12px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.5;
                            padding-top: 8px;
                            padding-bottom: 8px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.5;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 21px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                word-break: break-word;
                                text-align: center;
                                mso-line-height-alt: 30px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 24px;
                                  color: #333333;
                                  font-weight: bold;
                                  line-height: 1.25;
                                "
                                >Secret Key : ${token}</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="left"
                          class="button-container"
                          style="padding: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;"><tr><td style="padding-top: 24px; padding-right: 24px; padding-bottom: 12px; padding-left: 24px" align="left"><v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word" href="" style="height:31.5pt;width:171.75pt;v-text-anchor:middle;" arcsize="10%" stroke="false" fillcolor="#1d84ff"><w:anchorlock/><v:textbox inset="0,0,0,0"><center style="color:#ffffff; font-family:Arial, sans-serif; font-size:16px"><![endif]-->
                          <a
                            href="${registrationLink}"
                            target="_blank"
                          >
                            <div
                              style="
                                text-decoration: none;
                                display: inline-block;
                                color: #ffffff;
                                background-color: #1d84ff;
                                border-radius: 4px;
                                cursor: pointer;
                                -webkit-border-radius: 4px;
                                -moz-border-radius: 4px;
                                width: auto;
                                width: auto;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                text-align: center;
                                mso-border-alt: none;
                                word-break: keep-all;
                              "
                            >
                              <span
                                style="
                                  padding: 12px 16px;
                                  font-size: 14px;
                                  line-height: 1.14;
                                  letter-spacing: 0.16px;
                                  display: inline-block;
                                  letter-spacing: undefined;
                                "
                                >Register with Leucine</span
                              >
                            </div>
                          </a>
                          <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                color: #666666;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              You may copy paste this link into your web browser
                              -
                              <a
                                href="${registrationLink}"
                                target="_blank"
                                style="text-decoration: none"
                              >
                                <span style="color: #1d84ff"
                                  >${registrationLink}</span
                                >
                              </a>
                            </p>
                            <!-- <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >

                            </p> -->
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(16, 'RESET_PASSWORD', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >Your request to reset your Password
                                </span
                              >
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333"
                                >Hello ${userFullName},</span
                              ><br /><br /><span style="color: #333333"
                                >We have received a request to reset the passowrd for Leucine.</span
                              ><br /><br /><span style="color: #333333"
                                >You can reset the password by clicking the Reset Password button below and using the Secret Key to verify your identity.</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #ffffff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    background-color: #e7f1fd;
                    margin: 0 24px;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#e7f1fd"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#e7f1fd;width:480px; border-top: 0px solid #000000; border-left: 0px solid #000000; border-bottom: 0px solid #000000; border-right: 0px solid #000000;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid #000000;
                          border-left: 0px solid #000000;
                          border-bottom: 0px solid #000000;
                          border-right: 0px solid #000000;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 12px; padding-bottom: 12px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.5;
                            padding-top: 8px;
                            padding-bottom: 8px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.5;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 21px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                word-break: break-word;
                                text-align: center;
                                mso-line-height-alt: 30px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 24px;
                                  color: #333333;
                                  font-weight: bold;
                                  line-height: 1.25;
                                "
                                >Secret Key : ${token}</span
                              >
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="left"
                          class="button-container"
                          style="padding: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;"><tr><td style="padding-top: 24px; padding-right: 24px; padding-bottom: 12px; padding-left: 24px" align="left"><v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word" href="" style="height:31.5pt;width:171.75pt;v-text-anchor:middle;" arcsize="10%" stroke="false" fillcolor="#1d84ff"><w:anchorlock/><v:textbox inset="0,0,0,0"><center style="color:#ffffff; font-family:Arial, sans-serif; font-size:16px"><![endif]-->
                          <a
                            href="${resetPasswordLink}"
                            target="_blank"
                          >
                            <div
                              style="
                                text-decoration: none;
                                display: inline-block;
                                color: #ffffff;
                                background-color: #1d84ff;
                                border-radius: 4px;
                                cursor: pointer;
                                -webkit-border-radius: 4px;
                                -moz-border-radius: 4px;
                                width: auto;
                                width: auto;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                text-align: center;
                                mso-border-alt: none;
                                word-break: keep-all;
                              "
                            >
                              <span
                                style="
                                  padding: 12px 16px;
                                  font-size: 14px;
                                  line-height: 1.14;
                                  letter-spacing: 0.16px;
                                  display: inline-block;
                                  letter-spacing: undefined;
                                "
                                >Reset Password</span
                              >
                            </div>
                          </a>
                          <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                        style="
                          color: #555555;
                          font-family: Helvetica Neue, Helvetica, Arial,
                            sans-serif;
                          line-height: 1.2;
                          padding-right: 24px;
                          padding-bottom: 24px;
                          padding-left: 24px;
                        "
                      >
                        <div
                          class="txtTinyMce-wrapper"
                          style="
                            font-size: 14px;
                            line-height: 1.14;
                            letter-spacing: 0.16px;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            color: #555555;
                            mso-line-height-alt: 17px;
                          "
                        >
                          <p
                            style="
                              margin: 0;
                              font-size: 14px;
                              color: #333333;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              word-break: break-word;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                              margin-top: 0;
                              margin-bottom: 0;
                            "
                          >
                          If you did not request to reset your password, you can ignore this email.
                          </p>
                        </div>
                      </div>
                      <!--[if mso]></td></tr></table><![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                color: #666666;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              You may copy paste this link into your web browser
                              -
                              <a
                                href="${resetPasswordLink}"
                                target="_blank"
                                style="text-decoration: none"
                              >
                                <span style="color: #1d84ff"
                                  >${resetPasswordLink}</span
                                >
                              </a>
                            </p>
                            <!-- <p
                              style="
                                margin: 0;
                                font-size: 12px;
                                line-height: 1.33;
                                letter-spacing: 0.32px;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >

                            </p> -->
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(17, 'NOTIFY_ADMIN_INVITE_EXPIRED', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >Request to reset user''s</span
                              ><br /><span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                > Registration Invite</span
                              >
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333">Attention,</span>
                              <br /><br />
                              <span style="color: #333333">The following user in you organisation has requested to reset their Invitation to Register with Leucine App as their invitation has expired.</span>
                              <br /><br />
                              <span style="color: #333333">Employee Name: ${userFullName}</span>
                              <br />
                              <span style="color: #333333">Employee ID: ${employeeId}</span>
                              <br /><br />
                              <span style="color: #333333">If you consider this request to be valid, login to your Leucine account and Reset Invite for the user.</span>
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(18, 'NOTIFY_ADMIN_PASSWORD_RECOVERY_CHALLENGE_QUESTION_NOT_SET', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >Request to reset user''s password</span
                              ><br />
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333">Attention,</span>
                              <br /><br />
                              <span style="color: #333333">The following user in you organisation has requested a password reset to be performed on their account.</span>
                              <br /><br />
                              <span style="color: #333333">Employee Name: ${userFullName}</span>
                              <br />
                              <span style="color: #333333">Employee ID: ${employeeId}</span>
                              <br /><br />
                              <span style="color: #333333">If you consider this request to be valid, login to your Leucine account and Generate Secret Key for the user.</span>
                              <br /><br />
                              <span style="color: #333333">The generated Secret Key can be used by the user to reset their password.</span>
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(19, 'NOTIFY_ADMIN_PASSWORD_RECOVERY_ACCOUNT_LOCKED', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >Request to unlock user''s account</span
                              ><br />
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333">Attention,</span>
                              <br /><br />
                              <span style="color: #333333">The following user in you organisation has requested an unlock to be performed on their account.</span>
                              <br /><br />
                              <span style="color: #333333">Employee Name: ${userFullName}</span>
                              <br />
                              <span style="color: #333333">Employee ID: ${employeeId}</span>
                              <br /><br />
                              <span style="color: #333333">If you consider this request to be valid, login to your Leucine account and Unlock for the user.</span>
                              <br /><br />
                              <span style="color: #333333"><strong>NOTE: </strong>Due to multiple failed attempts to validate the challenge question and answer, during password recovery, the account was locked  </span>
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
INSERT INTO public.email_templates (id, "name", "content") VALUES(20, 'NOTIFY_ADMIN_PASSWORD_RECOVERY_KEY_EXPIRED', '
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:v="urn:schemas-microsoft-com:vml"
>
  <head>
    <!--[if gte mso 9
      ]><xml
        ><o:OfficeDocumentSettings
          ><o:AllowPNG /><o:PixelsPerInch
            >96</o:PixelsPerInch
          ></o:OfficeDocumentSettings
        ></xml
      ><!
    [endif]-->
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <meta content="width=device-width" name="viewport" />
    <!--[if !mso]><!-->
    <meta content="IE=edge" http-equiv="X-UA-Compatible" />
    <!--<![endif]-->
    <title></title>
    <!--[if !mso]><!-->
    <link
      href="https://fonts.googleapis.com/css?family=Open+Sans"
      rel="stylesheet"
      type="text/css"
    />
    <!--<![endif]-->
    <style type="text/css">
      @import url("https://fonts.googleapis.com/css2?family=Nunito+Sans:ital,wght@0,200;0,300;0,400;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,600;1,700;1,800;1,900&display=swap");
      * {
        font-family: Nunito, sans-serif !important;
      }
      body {
        margin: 0;
        padding: 0;
      }

      table,
      td,
      tr {
        vertical-align: top;
        border-collapse: collapse;
      }

      a[x-apple-data-detectors="true"] {
        color: inherit !important;
        text-decoration: none !important;
      }
    </style>
    <style id="media-query" type="text/css">
      @media (max-width: 500px) {
        .block-grid,
        .col {
          min-width: 320px !important;
          max-width: 100% !important;
          display: block !important;
        }

        .block-grid {
          width: 100% !important;
        }

        .col {
          width: 100% !important;
        }

        .col_cont {
          margin: 0 auto;
        }

        img.fullwidth,
        img.fullwidthOnMobile {
          max-width: 100% !important;
        }

        .no-stack .col {
          min-width: 0 !important;
          display: table-cell !important;
        }

        .no-stack.two-up .col {
          width: 50% !important;
        }

        .no-stack .col.num2 {
          width: 16.6% !important;
        }

        .no-stack .col.num3 {
          width: 25% !important;
        }

        .no-stack .col.num4 {
          width: 33% !important;
        }

        .no-stack .col.num5 {
          width: 41.6% !important;
        }

        .no-stack .col.num6 {
          width: 50% !important;
        }

        .no-stack .col.num7 {
          width: 58.3% !important;
        }

        .no-stack .col.num8 {
          width: 66.6% !important;
        }

        .no-stack .col.num9 {
          width: 75% !important;
        }

        .no-stack .col.num10 {
          width: 83.3% !important;
        }

        .video-block {
          max-width: none !important;
        }

        .mobile_hide {
          min-height: 0px;
          max-height: 0px;
          max-width: 0px;
          display: none;
          overflow: hidden;
          font-size: 0px;
        }

        .desktop_hide {
          display: block !important;
          max-height: none !important;
        }
      }
    </style>
    <style id="icon-media-query" type="text/css">
      @media (max-width: 500px) {
        .icons-inner {
          text-align: center;
        }

        .icons-inner td {
          margin: 0 auto;
        }
      }
    </style>
  </head>

  <body
    class="clean-body"
    style="
      margin: 0;
      padding: 0;
      -webkit-text-size-adjust: 100%;
      background-color: #f4f4f4;
    "
  >
    <!--[if IE]><div class="ie-browser"><![endif]-->
    <table
      bgcolor="#f4f4f4"
      cellpadding="0"
      cellspacing="0"
      class="nl-container"
      role="presentation"
      style="
        table-layout: fixed;
        vertical-align: top;
        min-width: 320px;
        border-spacing: 0;
        border-collapse: collapse;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        background-color: #f4f4f4;
        width: 100%;
      "
      valign="top"
      width="100%"
    >
      <tbody>
        <tr style="vertical-align: top" valign="top">
          <td style="word-break: break-word; vertical-align: top" valign="top">
            <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="background-color:#f4f4f4"><![endif]-->
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 16px; padding-left: 16px; padding-top:16px; padding-bottom:16px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 16px;
                          padding-bottom: 16px;
                          padding-right: 16px;
                          padding-left: 16px;
                        "
                      >
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <div
                          align="center"
                          class="img-container center fixedwidth"
                          style="padding-right: 24px; padding-left: 24px"
                        >
                          <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr style="line-height:0px"><td style="padding-right: 24px;padding-left: 24px;" align="center"><![endif]-->
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <a
                            href="${loginLink}"
                            style="outline: none"
                            tabindex="-1"
                            target="_blank"
                            ><img
                              align="center"
                              alt="Leucine"
                              border="0"
                              class="center fixedwidth"
                              src="cid:leucine-blue-logo"
                              style="
                                text-decoration: none;
                                -ms-interpolation-mode: bicubic;
                                height: auto;
                                border: 0;
                                width: 100%;
                                max-width: 120px;
                                display: block;
                              "
                              title="Leucine"
                              width="120"
                          /></a>
                          <div style="font-size: 1px; line-height: 24px"> </div>
                          <!--[if mso]></td></tr></table><![endif]-->
                        </div>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              color: #555555;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                font-size: 20px;
                                mso-line-height-alt: 24px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              <span
                                style="
                                  font-size: 20px;
                                  color: #333333;
                                  font-weight: 300;
                                  line-height: 1.2;
                                  letter-spacing: 0.16px;
                                "
                                >Request to reset user''s password</span
                              ><br />
                            </p>
                          </div>
                        </div>
                        <!--[if mso]></td></tr></table><![endif]-->
                        <table
                          border="0"
                          cellpadding="0"
                          cellspacing="0"
                          class="divider"
                          role="presentation"
                          style="
                            table-layout: fixed;
                            vertical-align: top;
                            border-spacing: 0;
                            border-collapse: collapse;
                            mso-table-lspace: 0pt;
                            mso-table-rspace: 0pt;
                            min-width: 100%;
                            -ms-text-size-adjust: 100%;
                            -webkit-text-size-adjust: 100%;
                          "
                          valign="top"
                          width="100%"
                        >
                          <tbody>
                            <tr style="vertical-align: top" valign="top">
                              <td
                                class="divider_inner"
                                style="
                                  word-break: break-word;
                                  vertical-align: top;
                                  min-width: 100%;
                                  -ms-text-size-adjust: 100%;
                                  -webkit-text-size-adjust: 100%;
                                  padding-top: 24px;
                                  padding-right: 24px;
                                  padding-bottom: 0px;
                                  padding-left: 24px;
                                "
                                valign="top"
                              >
                                <table
                                  align="center"
                                  border="0"
                                  cellpadding="0"
                                  cellspacing="0"
                                  class="divider_content"
                                  role="presentation"
                                  style="
                                    table-layout: fixed;
                                    vertical-align: top;
                                    border-spacing: 0;
                                    border-collapse: collapse;
                                    mso-table-lspace: 0pt;
                                    mso-table-rspace: 0pt;
                                    border-top: 1px solid #bbbbbb;
                                    width: 100%;
                                  "
                                  valign="top"
                                  width="100%"
                                >
                                  <tbody>
                                    <tr
                                      style="vertical-align: top"
                                      valign="top"
                                    >
                                      <td
                                        style="
                                          word-break: break-word;
                                          vertical-align: top;
                                          -ms-text-size-adjust: 100%;
                                          -webkit-text-size-adjust: 100%;
                                        "
                                        valign="top"
                                      >
                                        <span></span>
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 24px;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.2;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                line-height: 1.43;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                                letter-spacing: 0.16px;
                              "
                            >
                              <span style="color: #333333">Attention,</span>
                              <br /><br />
                              <span style="color: #333333">The following user in you organisation has requested a password reset to be performed on their account.</span>
                              <br /><br />
                              <span style="color: #333333">Employee Name: ${userFullName}</span>
                              <br />
                              <span style="color: #333333">Employee ID: ${employeeId}</span>
                              <br /><br />
                              <span style="color: #333333">If you consider this request to be valid, login to your Leucine account and Generate Secret Key for the user.</span>
                              <br /><br />
                              <span style="color: #333333">The generated Secret Key can be used by the user to reset their password.</span>
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: #fff;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: #fff;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:#fff"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:#fff;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 0px;
                          padding-bottom: 0px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 24px; padding-left: 24px; padding-top: 24px; padding-bottom: 24px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Helvetica Neue, Helvetica, Arial,
                              sans-serif;
                            line-height: 1.2;
                            padding-right: 24px;
                            padding-bottom: 24px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 14px;
                              line-height: 1.14;
                              letter-spacing: 0.16px;
                              font-family: Helvetica Neue, Helvetica, Arial,
                                sans-serif;
                              color: #555555;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                font-size: 14px;
                                color: #333333;
                                line-height: 1.14;
                                letter-spacing: 0.16px;
                                word-break: break-word;
                                font-family: Helvetica Neue, Helvetica, Arial,
                                  sans-serif;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              Warm Regards <br />Leucine App
                            </p>
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
            <div style="background-color: transparent">
              <div
                class="block-grid"
                style="
                  min-width: 320px;
                  max-width: 480px;
                  overflow-wrap: break-word;
                  word-wrap: break-word;
                  word-break: break-word;
                  margin: 0 auto;
                  background-color: transparent;
                "
              >
                <div
                  style="
                    border-collapse: collapse;
                    display: table;
                    width: 100%;
                    background-color: transparent;
                  "
                >
                  <!--[if (mso)|(IE)]><table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:transparent;"><tr><td align="center"><table cellpadding="0" cellspacing="0" border="0" style="width:480px"><tr class="layout-full-width" style="background-color:transparent"><![endif]-->
                  <!--[if (mso)|(IE)]><td align="center" width="480" style="background-color:transparent;width:480px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;" valign="top"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;"><![endif]-->
                  <div
                    class="col num12"
                    style="
                      min-width: 320px;
                      max-width: 480px;
                      display: table-cell;
                      vertical-align: top;
                      width: 480px;
                    "
                  >
                    <div class="col_cont" style="width: 100% !important">
                      <!--[if (!mso)&(!IE)]><!-->
                      <div
                        style="
                          border-top: 0px solid transparent;
                          border-left: 0px solid transparent;
                          border-bottom: 0px solid transparent;
                          border-right: 0px solid transparent;
                          padding-top: 5px;
                          padding-bottom: 5px;
                          padding-right: 0px;
                          padding-left: 0px;
                        "
                      >
                        <!--<![endif]-->
                        <!--[if mso]><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: Arial, sans-serif"><![endif]-->
                        <div
                          style="
                            color: #555555;
                            font-family: Arial, Helvetica Neue, Helvetica,
                              sans-serif;
                            line-height: 1.2;
                            padding-top: 10px;
                            padding-right: 24px;
                            padding-bottom: 10px;
                            padding-left: 24px;
                          "
                        >
                          <div
                            class="txtTinyMce-wrapper"
                            style="
                              font-size: 12px;
                              line-height: 1.2;
                              letter-spacing: 0.16px;
                              color: #666666;
                              font-family: Arial, Helvetica Neue, Helvetica,
                                sans-serif;
                              mso-line-height-alt: 17px;
                            "
                          >
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                               
                            </p>
                            <p
                              style="
                                margin: 0;
                                text-align: center;
                                line-height: 1.2;
                                word-break: break-word;
                                mso-line-height-alt: 17px;
                                margin-top: 0;
                                margin-bottom: 0;
                              "
                            >
                              To log in to Leucine open this link in your web
                              browser (e.g. Google Chrome) -
                              <a
                                href="${loginLink}"
                                target="_blank"
                                style="text-decoration: none"
                                ><span style="color: #1d84ff"
                                  >${loginLink}</span
                                ></a
                              >
                            </p>
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
  </body>
</html>
');
