package com.liferay.demo.commerce.upload.portlet.action;

import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.constants.CommerceWebKeys;
import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.currency.service.CommerceCurrencyLocalService;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.product.model.CommerceChannel;
import com.liferay.commerce.product.service.CommerceChannelLocalService;
import com.liferay.commerce.service.CommerceOrderItemLocalService;
import com.liferay.commerce.service.CommerceOrderLocalService;
import com.liferay.demo.commerce.upload.constants.CommerceBulkUploadPortletKeys;
import com.liferay.demo.commerce.upload.constants.MVCCommandNames;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.osgi.web.wab.generator.WabGenerator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * @author Lorenzo Carbone
 */
@Component(
    immediate = true, 
    property = {
        "javax.portlet.name=" + CommerceBulkUploadPortletKeys.COMMERCEBULKUPLOAD,
        "mvc.command.name=" + MVCCommandNames.ADD_TO_CART
    }, 
    service = MVCActionCommand.class
)
public class CommerceBulkUploadAddToCartMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		long companyId = themeDisplay.getCompanyId();

		String products = ParamUtil.getString(actionRequest, "products");
		String[] cpInstanceIds = products.split(",");

		CommerceContext commerceContext =
				(CommerceContext)actionRequest.getAttribute(
						CommerceWebKeys.COMMERCE_CONTEXT);

		ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

		// Create order
		CommerceAccount commerceAccount = commerceContext.getCommerceAccount();

		CommerceChannel commerceChannel = _commerceChannelLocalService.getCommerceChannel(commerceContext.getCommerceChannelId());

		CommerceOrder commerceOrder = _commerceOrderLocalService.addCommerceOrder(
				themeDisplay.getUserId(),
				commerceChannel.getGroupId(),
				commerceAccount.getCommerceAccountId(),
				_commerceCurrencyLocalService.getCommerceCurrency(companyId, commerceContext.getCommerceCurrency().getCode()).getCommerceCurrencyId());

		for (String cpInstanceId : cpInstanceIds) {
			_commerceOrderItemLocalService.addCommerceOrderItem(
					commerceOrder.getCommerceOrderId(), Long.parseLong(cpInstanceId), 1, 1, "", commerceContext,
					serviceContext);
		}

	}

	@Reference
	private CommerceOrderLocalService _commerceOrderLocalService;

	@Reference
	private CommerceOrderItemLocalService _commerceOrderItemLocalService;

	@Reference
	private CommerceCurrencyLocalService _commerceCurrencyLocalService;

	@Reference
	private CommerceChannelLocalService _commerceChannelLocalService;

	private static final Log log = LogFactoryUtil.getLog(WabGenerator.class);
}