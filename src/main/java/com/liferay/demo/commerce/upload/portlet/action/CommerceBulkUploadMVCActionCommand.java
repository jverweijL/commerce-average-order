package com.liferay.demo.commerce.upload.portlet.action;

import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.constants.CommerceOrderConstants;
import com.liferay.commerce.constants.CommerceWebKeys;
import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.inventory.engine.CommerceInventoryEngine;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.commerce.order.engine.CommerceOrderEngine;
import com.liferay.commerce.product.catalog.CPCatalogEntry;
import com.liferay.commerce.product.content.util.CPContentHelper;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.service.CPInstanceLocalService;
import com.liferay.commerce.product.util.CPDefinitionHelper;
import com.liferay.commerce.product.util.CPInstanceHelper;
import com.liferay.commerce.service.CommerceOrderLocalService;
import com.liferay.demo.commerce.upload.bean.ProductBean;
import com.liferay.demo.commerce.upload.constants.CommerceBulkUploadPortletKeys;
import com.liferay.demo.commerce.upload.constants.MVCCommandNames;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.osgi.web.wab.generator.WabGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Lorenzo Carbone
 */
@Component(
    immediate = true, 
    property = {
        "javax.portlet.name=" + CommerceBulkUploadPortletKeys.COMMERCEBULKUPLOAD,
        "mvc.command.name=" + MVCCommandNames.UPLOAD_CSV
    }, 
    service = MVCActionCommand.class
)
public class CommerceBulkUploadMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		HashMap<String,ProductBean> orderedItems = new HashMap<String, ProductBean>();
		HashMap<String,Integer> orderedItemsCounter = new HashMap<String, Integer>();
		List<ProductBean> products = new ArrayList<>();
		List<Long> productsIntances = new ArrayList<>();
		ProductBean product;
		int quantity;
		int count;
		long companyId = 0;


		try {
			ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
			companyId = themeDisplay.getCompanyId();

/*			UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(actionRequest);
			InputStream is = uploadRequest.getFileAsStream("csvDataFile");*/

			CommerceContext commerceContext =
					(CommerceContext) actionRequest.getAttribute(
							CommerceWebKeys.COMMERCE_CONTEXT);
			CommerceAccount commerceAccount = commerceContext.getCommerceAccount();
			long commerceAccountId = commerceAccount.getCommerceAccountId();

			List<CommerceOrder> commerceOrders = _CommerceOrderLocalService.getCommerceOrdersByCommerceAccountId(
					commerceAccountId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

			System.out.println("Found " + commerceOrders.size() + " orders for " + commerceAccountId);

			for (CommerceOrder commerceOrder : commerceOrders) {
				try {
					if (commerceOrder.getOrderStatus() == CommerceOrderConstants.ORDER_STATUS_COMPLETED) {
						_log.info("Found  a completed order " + commerceOrder.getCommerceOrderId());

						for (CommerceOrderItem commerceOrderItem : commerceOrder.getCommerceOrderItems()) {

							CPDefinition cpDefinition = commerceOrderItem.getCPDefinition();

							String sku = commerceOrderItem.getSku();
							quantity = commerceOrderItem.getQuantity();


							System.out.println("Found an item " + commerceOrderItem.getName(themeDisplay.getLocale()) + "(" + sku + ")" + " ...ordered " + quantity);

							for(CPInstance cpInstance:cpDefinition.getCPInstances()){
								productsIntances.add(cpInstance.getCPInstanceId());
							}

							if (orderedItems.containsKey(sku)) {
								System.out.println("update");
								product = orderedItems.get(sku);
								product.setQuantity(product.getQuantity() + quantity);
								count = orderedItemsCounter.get(sku) + 1;
								orderedItems.put(sku,product);
								orderedItemsCounter.put(sku, count);
							} else {
								System.out.println("new");
								product = new ProductBean();
								product.setName(commerceOrderItem.getName(themeDisplay.getLocale()));
								product.setSku(sku);
								product.setFriendlyURL(cpDefinition.getURL(themeDisplay.getLanguageId()));
								product.setThumbnailSrc(cpDefinition.getDefaultImageThumbnailSrc());
								product.setQuantity(quantity);
								orderedItems.put(sku,product);
								orderedItemsCounter.put(sku, 1);

							}

						}
					}
				} catch (Exception ex) {
					System.out.println("Error: try next order");
					System.out.println(ex.getMessage());
				}
			}
		} catch (Exception ex) {
			System.out.println("Some issue with getting the orders I guess");
			System.out.println(ex.getMessage());
		}

		if (orderedItems != null)
		{
			for (String sku:orderedItems.keySet()) {
				product = orderedItems.get(sku);
				product.setQuantity(product.getQuantity() / orderedItemsCounter.get(sku));
				int quantityInStock = commerceInventoryEngine.getStockQuantity(companyId, sku);
				product.setQuantityInStock(quantityInStock);
				System.out.printf("Ready to order %s(%s) \n",product.getName(),product.getQuantity());
				products.add(product);
			}

			actionRequest.setAttribute("cpInstanceHelper", _cpInstanceHelper);
			actionRequest.setAttribute("products", products);
			actionRequest.setAttribute("productsIntances", productsIntances);
			actionResponse.setRenderParameter(
					"mvcPath", "/products.jsp");

		}
		/*String filePath = "products.csv";
		try (FileOutputStream fOut = new FileOutputStream(filePath);) {
			int i;
			while ((i = is.read()) != -1) {
				fOut.write(i);
			}

			File csvFile = new File(filePath);

			if (Validator.isNotNull(csvFile)) {
				if (csvFile.getName().contains(".csv")) {

					List<ProductBean> products = new ArrayList<>();
					List<Long> productsIntances = new ArrayList<>();

					for (CSVRecord csvRecord : _getCSVParser(csvFile)) {
						String sku = csvRecord.get("sku");
						String quantity = csvRecord.get("quantity");

						List<CPInstance> cpInstances = _cpInstanceLocalService.searchCPInstances(companyId, sku,
								WorkflowConstants.STATUS_APPROVED, QueryUtil.ALL_POS,
								QueryUtil.ALL_POS, null).getBaseModels();

						for (CPInstance cpInstance : cpInstances) {

							if(StringUtil.equals(cpInstance.getSku(), sku)){

								CPCatalogEntry cpCatalogEntry =
										_cpDefinitionHelper.getCPCatalogEntry(
												commerceAccount.getCommerceAccountId(),
												commerceContext.getCommerceChannelGroupId(), cpInstance.getCPDefinition().getCPDefinitionId(),
												themeDisplay.getLocale());
								String friendlyURL = _cpContentHelper.getFriendlyURL(cpCatalogEntry, themeDisplay);

								productsIntances.add(cpInstance.getCPInstanceId());

								ProductBean product = new ProductBean();
								product.setName(cpInstance.getCPDefinition().getName());
								product.setSku(sku);
								product.setFriendlyURL(friendlyURL);
								product.setThumbnailSrc(_cpInstanceHelper.getCPInstanceThumbnailSrc(cpInstance.getCPInstanceId()));
								product.setQuantity(Integer.parseInt(quantity));

								int quantityInStock = commerceInventoryEngine.getStockQuantity(companyId, sku);
								product.setQuantityInStock(quantityInStock);
								products.add(product);
							}
						}
					}

					actionRequest.setAttribute("cpInstanceHelper", _cpInstanceHelper);

					actionRequest.setAttribute("products", products);
					actionRequest.setAttribute("productsIntances", productsIntances);
					actionResponse.setRenderParameter(
							"mvcPath", "/products.jsp");

				} else {
					log.error("Uploaded File is not CSV file.Your file name is ----> " + csvFile.getName());
				}

			}
		} catch (Exception e) {
			log.error("Exception in CSV File Reading Process :: ", e);
		}*/

	}

	/*private CSVParser _getCSVParser(File csvFile) throws Exception {
		CSVFormat csvFormat = CSVFormat.DEFAULT;
		csvFormat = csvFormat.withFirstRecordAsHeader();
		csvFormat = csvFormat.withIgnoreSurroundingSpaces();
		csvFormat = csvFormat.withNullString(StringPool.BLANK);
		try {
			return CSVParser.parse(
					csvFile, Charset.defaultCharset(), csvFormat);
		}
		catch (IOException ioException) {
			log.error(ioException, ioException);
			throw ioException;
		}
	}*/

	@Reference
	private CommerceInventoryEngine commerceInventoryEngine;

	@Reference
	private CPInstanceLocalService _cpInstanceLocalService;

	@Reference
	private CPInstanceHelper _cpInstanceHelper;

	@Reference
	private CPContentHelper _cpContentHelper;

	@Reference
	private CPDefinitionHelper _cpDefinitionHelper;

	@Reference
	private CommerceOrderEngine _CommerceOrderEngine;

	@Reference
	private CommerceOrderLocalService _CommerceOrderLocalService;



	private static final Log _log = LogFactoryUtil.getLog(WabGenerator.class);
}